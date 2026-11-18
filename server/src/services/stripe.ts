import Stripe from "stripe";
import { config } from "../config";

export class StripeServiceError extends Error {
  readonly code: string;
  readonly statusCode: number;

  constructor(code: string, message: string, statusCode = 502) {
    super(message);
    this.name = "StripeServiceError";
    this.code = code;
    this.statusCode = statusCode;
  }
}

export interface CreatePaymentIntentParams {
  amountVnd: number;
  draftId: string;
  uid: string;
  accountId: string;
}

export interface CreatePaymentIntentResult {
  stripeIntentId: string;
  clientSecret: string;
}

export interface RetrievedPaymentIntent {
  id: string;
  status: string;
  amount: number;
  currency: string;
}

export interface StripeClientLike {
  paymentIntents: {
    create(
      params: Stripe.PaymentIntentCreateParams,
      options?: Stripe.RequestOptions
    ): Promise<Stripe.PaymentIntent>;
    retrieve(id: string): Promise<Stripe.PaymentIntent>;
  };
}

export interface StripeServiceDeps {
  stripeClient?: StripeClientLike;
  secretKey?: string;
}

export interface StripeService {
  createPaymentIntent(params: CreatePaymentIntentParams): Promise<CreatePaymentIntentResult>;
  retrievePaymentIntent(paymentIntentId: string): Promise<RetrievedPaymentIntent>;
}

export function createStripeService(deps?: StripeServiceDeps): StripeService {
  let cachedClient: StripeClientLike | undefined = deps?.stripeClient;

  function getClient(): StripeClientLike {
    if (cachedClient) {
      return cachedClient;
    }

    const secretKey = (deps?.secretKey ?? config.stripeSecretKey).trim();
    if (!secretKey) {
      throw new StripeServiceError(
        "SERVICE_UNAVAILABLE",
        "STRIPE_SECRET_KEY chưa được cấu hình trên máy chủ.",
        503
      );
    }

    cachedClient = new Stripe(secretKey, { typescript: true });
    return cachedClient;
  }

  return {
    async createPaymentIntent(
      params: CreatePaymentIntentParams
    ): Promise<CreatePaymentIntentResult> {
      const { amountVnd, draftId, uid, accountId } = params;
      if (!Number.isSafeInteger(amountVnd) || amountVnd <= 0) {
        throw new StripeServiceError(
          "INVALID_ARGUMENT",
          "Số tiền nạp qua Stripe phải là số nguyên dương (VNĐ).",
          400
        );
      }
      if (!draftId.trim() || !uid.trim() || !accountId.trim()) {
        throw new StripeServiceError(
          "INVALID_ARGUMENT",
          "Thiếu thông tin draftId, uid hoặc accountId khi tạo Stripe PaymentIntent.",
          400
        );
      }

      const client = getClient();
      try {
        // VND là tiền tệ không có phần thập phân (zero-decimal) trên Stripe: truyền trực tiếp amountVnd.
        const intent = await client.paymentIntents.create(
          {
            amount: amountVnd,
            currency: "vnd",
            automatic_payment_methods: { enabled: true },
            metadata: {
              draftId,
              uid,
              accountId,
            },
          },
          {
            idempotencyKey: `novapay_draft_${draftId}`,
          }
        );

        if (!intent.client_secret) {
          throw new StripeServiceError(
            "STRIPE_ERROR",
            "Stripe không trả về client_secret cho PaymentIntent.",
            502
          );
        }

        return {
          stripeIntentId: intent.id,
          clientSecret: intent.client_secret,
        };
      } catch (err) {
        if (err instanceof StripeServiceError) {
          throw err;
        }
        const message = err instanceof Error ? err.message : String(err);
        throw new StripeServiceError("STRIPE_ERROR", `Lỗi Stripe khi tạo PaymentIntent: ${message}`, 502);
      }
    },

    async retrievePaymentIntent(paymentIntentId: string): Promise<RetrievedPaymentIntent> {
      const trimmedId = paymentIntentId.trim();
      if (!trimmedId) {
        throw new StripeServiceError("INVALID_ARGUMENT", "Thiếu mã PaymentIntent cần kiểm tra.", 400);
      }

      const client = getClient();
      try {
        const intent = await client.paymentIntents.retrieve(trimmedId);
        return {
          id: intent.id,
          status: intent.status,
          amount: intent.amount,
          currency: intent.currency,
        };
      } catch (err) {
        if (err instanceof StripeServiceError) {
          throw err;
        }
        const message = err instanceof Error ? err.message : String(err);
        throw new StripeServiceError(
          "STRIPE_ERROR",
          `Lỗi Stripe khi truy vấn PaymentIntent: ${message}`,
          502
        );
      }
    },
  };
}

export const stripeService = createStripeService();
