# TON DUC THANG UNIVERSITY
**Faculty of Information Technology**  
**Course:** Mobile Apps Development  
**Final Project**

---

## Overview

In the era of Technology 4.0, banks and financial firms must provide clients with a convenient mechanism to perform all financial activities with ease. One method is e-banking, in which clients can operate and interact with the banks or financial firms via Web applications. The other method is to provide a banking app on mobile devices, which are available to everyone and accessible everywhere. Using the mobile app, clients do not need to go to ATM machines to withdraw money or transfer money to other people at any time without going to the banks and talking with the bank officers.

In Vietnam, all the banks such as Vietcombank, BIDV and OCB have provided mobile apps for their customers. The customers can download the apps from Google Play or the Apple App Store and install them on their mobile devices. The banks only need to provide the initial setup for the accounts. Afterwards, the customers can perform all banking activities on their own.

In this project, you will develop a mobile banking app to boost the operations of your beloved bank and improve its customers' experience. You can name your bank and your app with any name of your preference.

---

## Business Requirements

The app should provide the following main features:

1. **User profiles:** The app should support two types of users: bank customers and bank officers. The two kinds of users should be provided with different interfaces and permissions to perform different operations with the bank. The bank officers should be able to create accounts for customers and have permissions to modify customers' data upon customers' requests. Customers can view their banking information and modify it.

2. **Security requirements:** The app should provide a login screen to allow users to log in to the system. The system should apply eKYC (Electronic Know Your Customer) to identify the customers and users. The bank officers can support customers in providing identification information using biometric data. For example, the customers can be asked to scan their faces and upload the pictures to the system. The image will be used to verify them when performing any banking activities with high values. All transactions must pass two-factor authentication (2FA) to be persisted in the system. For example, to perform a transaction, users need to provide an OTP (one-time password) to verify their identity.

3. **Account management:** The customers should be able to view information about their accounts. There are three possible types of accounts: checking, saving, and mortgage. For each type of account, the customers should be provided with a different view of the information.
   - They can view the balance of checking and saving accounts.
   - The customers can view profitable rates and profits per month of saving accounts.
   - The banking officers can modify the profitable rates according to the bank's policies.
   - The users can view the amount of money they need to pay each month or every two weeks for their mortgage accounts.
   - The users can view the transactional history of their accounts.
   - The users can deposit (add money) or withdraw from their accounts.

4. **Transaction management:** The users can perform the banking transactions following security requirements in section 2. The system should verify if a transaction is allowed and valid before persisting it in the database. Users should be able to perform money transfers to other accounts in the same bank or to other accounts of different banks. The system should be integrated with payment services such as VNPay or Stripe.

5. **Utilities:** The app should provide the following utilities for the users:
   - Pay for electricity and water bills.
   - Deposit (add money) to their phone account.
   - Buy flight tickets, movie tickets, book hotel rooms or pay for products on an e-commerce platform.

6. **Navigation:** The users should be able to locate their location. The app provides a view of users on the map and the locations of all nearby bank offices. The app provides intelligent recommendations for the shortest walking path to the nearest bank office.

7. **AI chatbot:** Integrate an intelligent chatbot to interact with users. Users can ask questions in the chat screen. The chatbot provides answers with links to the requested activities: pay for phone services, pay for monthly bills, view transactions of the month and other financial activities.

---

## Technical Requirements

1. The project should be conducted by a team of 2-3 members. The team should develop an Android app to accomplish all requirements in the business requirement section.
2. The system should store all data in a database. It could be an SQLite database on the device. But it is preferred that the app use a database stored on a cloud computing service such as Google Firebase.
3. The app should be deployed on the Google Play Store. Any user can download the app, install and use it on his/her own smartphone.
4. The team should provide a project report with the following information:
   - **Analysis and Design:** explanation of the software development principles, patterns and practices being applied. Include all class diagrams, entity-relationship database diagrams, use cases, and solution diagrams for the components, infrastructure design, if any.
   - **Implementation:** explanation of the code structure and technologies used in the project.
   - **User Interface:** present all functionalities of your app.
5. Provide a demo video to show all the nice features of your app.