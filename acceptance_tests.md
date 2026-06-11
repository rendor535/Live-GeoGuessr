# Registration
## 1. Register using email and password
Given the user has internet connection

When:
1) Click "Don't have an account? Register" button
2) Fill in the forms with correct data
3) Click register

Then:
1) Correct forms are displayed
2) Application creates an account
3) Main screen is displayed

## 2. Register using Google account
Given the user has internet connection

When:
1) Click "Log in with Google"
2) Select a Google account

Then:
1) Application displays user's Google accounts to choose
2) Application creates or links an account
3) Main screen is displayed

## 3. Register using email and password with invalid data
Given the user has internet connection

When:
1) Click "Don't have an account? Register" button
2) Fill in the forms with incorrect data
3) Click register

Then:
1) Account is not created
2) Warning message is displayed
3) User remains on registration screen