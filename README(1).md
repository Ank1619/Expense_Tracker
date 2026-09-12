# 💰 Expense Tracker

A modern Android Expense Tracker that automatically detects financial transactions from SMS, categorizes expenses, tracks budgets, provides analytics, and helps users understand their spending habits.

The application is designed with a **local-first architecture**, using Room Database for persistent expense and transaction data.

---

## 📱 Overview

Expense Tracker reduces the effort of manually recording expenses by automatically detecting transaction SMS messages and extracting important information such as:

- Transaction amount
- Merchant
- Transaction ID
- Date
- Transaction type

When the application cannot determine the appropriate category, the transaction is placed into a **Pending Transactions** queue so the user can categorize it manually.

The application also provides budgeting, analytics, charts, search, filtering, and other personal-finance features.

---

## ✨ Features

### ✅ Automatic SMS Transaction Detection

The application listens for incoming transaction SMS messages and identifies messages containing financial transaction keywords.

Supported transaction indicators include:

- Debit
- Credit
- UPI
- Payment
- Purchase
- Paid
- Spent
- Transaction

The application extracts:

- 💰 Amount
- 🏪 Merchant
- 🔑 Transaction ID
- 📅 Transaction date

---

### ✅ Automatic Merchant Categorization

The application remembers the category selected by the user for a merchant.

For example:

```text
ABC MART → Shopping
```

When another transaction from `ABC MART` is detected, the application can automatically assign:

```text
Category → Shopping
```

This reduces repeated manual categorization.

---

### ✅ Unknown Merchant Handling

When a merchant cannot be categorized automatically:

```text
Transaction
     ↓
Merchant not recognized
     ↓
Pending Transaction
     ↓
User selects category
     ↓
Category saved
     ↓
Merchant-category mapping remembered
```

If the user skips categorization, the transaction can be placed into the `Other` category.

---

### ✅ Pending Transactions

Transactions that require user input are stored separately.

The application provides a dedicated **Pending Transactions** screen where users can:

- View pending transactions
- See merchant
- See amount
- See date
- Open a pending transaction
- Select its category
- Save it as an expense

The list refreshes automatically when the screen becomes visible again.

---

### ✅ Manual Expense Entry

Users can manually add expenses with:

- Expense name
- Amount
- Category
- Date

The date picker defaults to the current date.

Input validation prevents invalid or empty expense data from being stored.

---

### ✅ Expense Search

Users can search expenses by:

- Merchant
- Expense title

Example:

```text
Search: Amazon
```

The application displays matching expenses.

---

### ✅ Category Filtering

Expenses can be filtered by category.

Example categories include:

- Food
- Shopping
- Transport
- Entertainment
- Health
- Other

---

### ✅ Date Filtering

Users can filter expenses based on transaction/expense dates.

This makes it easier to analyze spending over a specific period.

---

### ✅ Amount Sorting

Expenses can be sorted based on amount:

```text
Highest → Lowest
```

or:

```text
Lowest → Highest
```

---

### ✅ Clear Filters

A clear-filter option allows users to quickly return to the complete expense list.

---

### ✅ Budget Management

Users can define a monthly spending budget.

The application tracks:

```text
Monthly Budget
       ↓
Current Spending
       ↓
Remaining Budget
       ↓
Budget Usage %
```

Example:

```text
Monthly Budget: ₹30,000
Spent:          ₹18,450
Remaining:      ₹11,550
Usage:          61.5%
```

---

### ✅ Expense Analytics

The Analytics dashboard provides an overview of spending.

It includes:

- Total spending
- Number of transactions
- Top spending category
- Category-based spending analysis
- Monthly spending analysis

---

### 📊 Spending by Category Chart

A category chart visually represents how expenses are distributed.

Example:

```text
Food          ₹4,200
Shopping      ₹8,450
Transport     ₹3,100
Entertainment ₹2,300
Health        ₹1,800
Other         ₹5,000
```

---

### 📈 Monthly Spending Chart

The application can visualize monthly spending trends to help users understand how their expenses change over time.

```text
Jan → ₹12,000
Feb → ₹15,500
Mar → ₹13,800
Apr → ₹18,200
```

---

# 🚧 Upcoming Features

The following features are planned for the next development phase.

## 1. Expense Details Screen

A dedicated screen will display complete information about an expense.

Planned information:

- Merchant/title
- Amount
- Category
- Date
- Payment source
- Notes
- Transaction ID

---

## 2. Payment Source Detection

The application will detect the payment method where possible.

Supported sources may include:

- UPI
- Debit Card
- Credit Card
- Bank Transfer
- Cash
- Other

Example:

```text
ABC MART
₹650
Shopping
UPI
```

---

## 3. Smart Spending Insights

The application will analyze spending data and provide useful insights.

Examples:

> Food spending increased compared with the previous month.

> Shopping is currently your highest spending category.

> Your average daily spending is ₹620.

> You are approaching your monthly budget.

The initial implementation will use local calculations rather than requiring an external AI service.

---

## 4. Export Reports

Users will be able to export their financial data.

Planned formats:

- CSV
- PDF

Users will also be able to share generated reports.

---

## 5. Dark Mode

The application will support:

- Light mode
- Dark mode
- System default

The selected preference will persist across app sessions.

---

## 6. App Lock

The application will provide an optional security layer for protecting financial information.

Planned functionality:

- PIN protection
- Lock when reopening the application
- Secure access to financial data

---

## 7. Receipt Scanner

Users will be able to scan receipts using the device camera.

The planned flow is:

```text
Capture Receipt
      ↓
Read Receipt
      ↓
Extract Information
      ↓
Merchant
Amount
Date
      ↓
User Verification
      ↓
Save Expense
```

The user will be able to verify extracted information before saving the expense.

---

# 🏗️ Architecture

The project uses a local Android architecture centered around **Room Database**.

High-level flow:

```text
                 ┌──────────────────┐
                 │   Transaction SMS│
                 └────────┬─────────┘
                          ↓
                 ┌──────────────────┐
                 │ SMS Broadcast    │
                 │ Receiver         │
                 └────────┬─────────┘
                          ↓
                 ┌──────────────────┐
                 │ Transaction      │
                 │ Detection        │
                 └────────┬─────────┘
                          ↓
                 ┌──────────────────┐
                 │ Merchant         │
                 │ Categorization   │
                 └───────┬──────────┘
                         ↓
              ┌──────────┴──────────┐
              │                     │
         Known Merchant        Unknown Merchant
              │                     │
              ↓                     ↓
       Save Expense          Pending Transaction
                                    │
                                    ↓
                            User Categorization
                                    │
                                    ↓
                              Save Expense
```

---

# 🗄️ Database

The application uses **Room** for local persistence.

Current database entities include:

```text
ExpenseEntity
MerchantCategoryEntity
PendingTransactionEntity
```

The database uses migrations to safely evolve the schema between versions.

Current database version:

```text
Version 3
```

---

# 📦 Main Components

## MainActivity

Responsible for the primary expense dashboard.

Responsibilities include:

- Display expenses
- Display total spending
- Add expenses
- Delete expenses
- Search expenses
- Filter expenses
- Sort expenses
- Open Pending Transactions
- Open Analytics

---

## TransactionSmsReceiver

Responsible for receiving incoming SMS transaction messages.

It:

1. Receives SMS
2. Reads the sender
3. Reads the message body
4. Determines whether the message looks like a transaction
5. Extracts transaction information
6. Determines merchant/category
7. Stores the transaction or creates a pending transaction

---

## UnknownMerchantActivity

Handles transactions where the merchant/category cannot be automatically determined.

Users can:

- Review transaction information
- Select a category
- Save the expense
- Skip categorization

Merchant-category preferences can then be remembered for future transactions.

---

## PendingTransactionsActivity

Displays transactions waiting for user categorization.

Users can open a transaction and continue the categorization process.

---

## AnalyticsActivity

Provides the analytics dashboard.

It calculates information from the Room database including:

- Total expenses
- Transaction count
- Category totals
- Top spending category
- Monthly spending

---

# 🛠️ Technology Stack

| Technology          | Purpose                      |
| ------------------- | ---------------------------- |
| Kotlin              | Primary programming language |
| Android SDK         | Application platform         |
| Android Studio      | Development environment      |
| Room                | Local database               |
| SQLite              | Local persistence layer      |
| Kotlin Coroutines   | Background operations        |
| Kotlin Flow         | Reactive database updates    |
| RecyclerView        | Expense lists                |
| Material Components | UI components                |
| MPAndroidChart      | Analytics charts             |
| BroadcastReceiver   | SMS transaction detection    |
| Git                 | Version control               |
| GitHub              | Source-code hosting          |

---

# 🔐 Permissions

The application requires SMS permission for automatic transaction detection.

Current permission:

```xml
<uses-permission
    android:name="android.permission.RECEIVE_SMS" />
```

Telephony hardware is marked as optional so the application can still be installed on devices without telephony hardware.

---

# 📱 Testing

The application has been tested using an Android Emulator.

ADB can be used to simulate incoming SMS messages during development.

Example:

```bash
adb emu sms send 9876543210 "Your A/C XX1234 has been debited by Rs. 999 at TEST MART"
```

The SMS receiver can then detect the transaction and process it through the normal transaction pipeline.

---

# 🚀 Getting Started

## Prerequisites

Install:

- Android Studio
- Android SDK
- JDK compatible with the project's Gradle configuration
- Android Emulator or Android device

---

## Clone the Repository

```bash
git clone <your-repository-url>
```

Open the project in Android Studio.

Allow Gradle to sync and build the project.

---

## Run the Application

1. Start an Android Emulator or connect an Android device.
2. Grant SMS permission when requested.
3. Build the project.
4. Run the application.
5. Add a manual expense or test the SMS transaction detector.

---

# 🧪 SMS Transaction Testing

For emulator testing, verify that ADB recognizes the emulator:

```bash
adb devices
```

Expected:

```text
List of devices attached
emulator-5554    device
```

Then send a test SMS:

```bash
adb emu sms send 9876543210 "Your A/C XX1234 has been debited by Rs. 500 at ABC MART"
```

Check Logcat using the tag:

```text
TransactionSMS
```

Expected logs include information similar to:

```text
Sender: 9876543210
Message: Your A/C XX1234 has been debited by Rs. 500 at ABC MART
TRANSACTION SMS DETECTED
TRANSACTION AMOUNT: ₹500
MERCHANT: ABC
```

---

# 📂 Project Structure

A simplified project structure:

```text
app/
├── src/
│   └── main/
│       ├── java/
│       │   └── com.example.expensestracker/
│       │       ├── MainActivity.kt
│       │       ├── AnalyticsActivity.kt
│       │       ├── PendingTransactionsActivity.kt
│       │       ├── UnknownMerchantActivity.kt
│       │       ├── TransactionSmsReceiver.kt
│       │       └── PendingTransactionAdapter.kt
│       │
│       ├── res/
│       │   ├── layout/
│       │   ├── drawable/
│       │   ├── values/
│       │   └── xml/
│       │
│       └── AndroidManifest.xml
```

---

# 🎯 Project Goals

The main goals of this project are:

- Reduce manual expense entry
- Automatically detect transactions
- Automatically categorize known merchants
- Provide useful spending analytics
- Help users stay within budgets
- Make personal finance tracking simple
- Provide a strong real-world Android development project

---

# 🔮 Future Roadmap

```text
[x] Manual Expense Entry
[x] Room Database
[x] SMS Transaction Detection
[x] Amount Extraction
[x] Merchant Extraction
[x] Merchant Category Mapping
[x] Unknown Merchant Handling
[x] Pending Transactions
[x] Search
[x] Category Filtering
[x] Date Filtering
[x] Amount Sorting
[x] Clear Filters
[x] Budget Management
[x] Analytics Dashboard
[x] Category Chart
[x] Monthly Spending Chart

[ ] Expense Details Screen
[ ] Payment Source Detection
[ ] Smart Spending Insights
[ ] CSV Export
[ ] PDF Export
[ ] Dark Mode
[ ] App Lock
[ ] Receipt Scanner
[ ] Backup & Restore
[ ] Advanced Notifications
```

---

# 👨‍💻 Development Philosophy

The project follows a modular and incremental development approach.

Core principles:

- Keep financial data locally available
- Use Room for reliable persistence
- Use asynchronous database operations
- Avoid blocking the main UI thread
- Validate user input
- Separate pending transactions from completed expenses
- Build features without unnecessarily coupling them together

---

# ⚠️ Privacy Considerations

This application processes financial transaction SMS messages.

Because transaction messages can contain sensitive financial information, production versions should carefully consider:

- Permission handling
- Secure local storage
- Data encryption
- Minimal data collection
- User consent
- Secure backups
- Safe export/sharing
- Protection of transaction information

SMS access should only be used for functionality that genuinely requires it.

---

# 📄 License

This project is currently intended for learning and portfolio purposes.

Add your preferred open-source license here before publishing the repository publicly.

---

# ⭐ Project Status

**Status: Active Development**

The core expense tracking, SMS transaction detection, merchant categorization, pending transaction workflow, search/filtering, budgeting, and analytics functionality are implemented.

Additional advanced features are being developed incrementally.
