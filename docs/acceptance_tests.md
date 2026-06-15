# Registration, logging in, logging out and deleting an account
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

## 4. Log in with email and password with valid data
Given the user has internet connection and is not logged in

When:
1) Fill in the forms with valid data
2) Click "Log in"

Then:
1) Account is linked
2) Main screen is displayed

## 5. Log in with email and password with invalid data
Given the user has internet connection and is not logged in

When:
1) Fill in the forms with invalid data
2) Click "Log in"

Then:
1) Account is not linked
2) Error message is displayed
3) Main screen is not displayed

## 5. Log out
Given the user has internet connection, is logged in and on the main screen

When:
1) Click the profile icon in bottom right
2) Click the settings icon in top right
3) Click "Log out"

Then:
1) Account is unlinked
2) Logging screen is displayed
3) User becomes logged out

## 6. Delete account
Given the user has internet connection, is logged in and on the main screen

When:
1) Click the profile icon in bottom right
2) Click the settings icon in top right
3) Click "Delete account"

Then:
1) Account no longer exists
2) Logging screen is displayed
3) User becomes logged out

# Friends
## 1. Sending a friend invitation from empty friends list state
Given the user has internet connection, is logged in and on the main screen

When:
1) Click "Add a friend"
2) Type in a friend's name
3) Click "Add"

Then:
1) Friend search screen is displayed
2) Accounts with matching nicknames are displayed
3) Application sends out a friend invite

## 2. Sending a friend invitation from populated friends list state
Given the user has internet connection, is logged in and on the main screen

When:
1) Click the profile icon in bottom right
2) Click "Add a Friend"
3) Type in a friend's name
4) Click "Add"

Then:
2) Friend search screen is displayed
3) Accounts with matching nicknames are displayed
4) Application sends out a friend invite

## 3. Removing a friend
Given the user has internet connection, is logged in, on the main screen and has a friend

When:
1) Click the profile icon in bottom right
2) Click "Friends"
3) Click "Remove"

Then:
2) User's friends list is displayed
3) Application removes selected person from user's friends list

## 4. Accepting a friend invitation
Given the user has internet connection, is logged in, on the main screen and has a pending invitation

When:
1) Click the profile icon in bottom right
2) Click "Friends"
3) Click "Accept"

Then:
1) User's friends list is displayed
2) Application removes that invitation
3) User no longer has the pending invite
4) Person is added to user's friends list

## 5. Rejecting a friend invitation
Given the user has internet connection, is logged in, on the main screen and has a pending invitation

When:
1) Click the profile icon in bottom right
2) Click "Friends"
3) Click "Reject"

Then:
1) User's friends list is displayed
2) Application removes that invitation
3) User no longer has the pending invite
4) Person is not added to user's friends list

## 6. Searching for a non-existent user
Given the user has internet connection, is logged in and on the main screen

When:
1) Click the profile icon in bottom right
2) Click "Add a Friend"
3) Type in a non-existing nickname

Then:
1) Friends search screen is displayed
2) Application does not show any results of search

# Profile customization
## 1. Changing avatar
Given the user has internet connection, is logged in and on the main screen

When:
1) Click the profile icon in bottom right
2) Click on user's profile picture
3) Select an image

Then:
1) User's profile screen is displayed
2) User's profile picture is changed

## 2. Nickname change
Given the user has internet connection, is logged in and on the main screen

When:
1) Click the profile icon in bottom right
2) Click on user's nickname
3) Type in new nickname
4) Click "Confirm"

Then:
1) Profile screen is displayed
2) Nickname is changed

# Gamemodes
## 1. Guessing
Given the user has internet connection, is logged in, has friends who have posted pictures and is on the main screen

When:
1) Click any post on the main screen
2) Click on the map
3) Place a waypoint on the map
4) Confirm guess

Then:
1) Application calculates how far off the location the user was
2) Application calculates amount of points to give to user
3) Real location along with user's guess is displayed on the map
4) A button to continue to main screen is displayed
5) Points are added to user's profile
6) Post is marked as guessed
7) Post is no longer visible in main menu
8) Post can be viewed in Guesses screen from user's profile

## 2. Posting
Given the user has internet connection, GPS, is logged in and is on the main screen

When:
1) Click the camera icon in bottom middle
2) Take a picture
3) Click the post button
4) Click the confirm button

Then:
1) Post is available for user's friends to be guessed
2) Post is added to user's Posts from profile
3) Camera screen is displayed

## 2. Posting without GPS on
Given the user has internet connection, is logged in and is on the main screen

When:
1) Click the camera icon in bottom middle
2) Take a picture
3) Give GPS permission to the application
4) Click the post button
5) Click the confirm button

Then:
1) Application asks user to give GPS permissions
2) Post is available for user's friends to be guessed
3) Post is added to user's Posts from profile
4) Camera screen is displayed