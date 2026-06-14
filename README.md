# ![alt text](https://i.imgur.com/SWOe052.png "Live GeoGuessr") Live-GeoGuessr
## A game where you can guess where your friends are (or were)!
Show off your geo-locating skills to your friends by guessing where they took their picture at and gain points! Or boast where you were last weekend by posting a picture yourself!

### Guess where your friend took the picture and gain points!
Create an account, add friends and see what they were up to

![alt text](https://i.imgur.com/VqQY20I.jpeg "Logging screen") ![alt text](https://i.imgur.com/NgdPhGO.jpeg "Main screen no friends") ![alt text](https://i.imgur.com/fhyOvjz.jpeg "Searching for friends") ![alt text](https://i.imgur.com/GJevgEv.jpeg "Friends added")  ![alt text](https://i.imgur.com/aDYSvNZ.png "Main screen") 

![alt text](https://i.imgur.com/XfnAuPz.png "Live GeoGuessr") ![alt text](https://i.imgur.com/2arwWpo.png "Guessing") 

### Or post a picture yourself!
By choosing the middle camera icon on the bottom and giving the GPS permissions

![alt text](https://i.imgur.com/8qQhcv0.jpeg "Live GeoGuessr") ![alt text](https://i.imgur.com/DH3MOGJ.jpeg "Live GeoGuessr")  ![alt text](https://i.imgur.com/zYud8Fv.jpeg "Live GeoGuessr") ![alt text](https://i.imgur.com/ni3vNlM.jpeg "Live GeoGuessr")  

![alt text](https://i.imgur.com/N40KJwB.png "Live GeoGuessr")

#### Why smartphone
The application needs a smartphone to quickly take photos in public places and assess where it was taken through GPS, providing high acccessibility at any moment. Unless you want to carry a notebook everywhere...

#### Target group
The typical user is any person with a mobile phone with internet connection and (optionally) camera, and GPS, who wants to spend a few to tens of minutes having fun by guessing locations based off of user-taken photographs.

#### MVP vs extra functionalities
MVP:
- Create an account
- Delete your account
- Post a picture
- Guess the location of a picutre
- Basic points system for correctly guessing

Additional functionalities:
- Edit profile (done)
- Adding friends (done)
- Removing friends (done)
- More advanced points system, ranking among friends
- Levels

#### User stories with acceptance criteria
1. As an unregistered user, I want to be able to create an account, so I can use the application.
	- Registration through Google account (user must have a Google account to use the app)
	- After registrating, user is taken to main window after a successful registration
	- Internet connection required for Google synchronization
	- No extra forms, just an option to register through Google account (at least for now)
2. As a logged out user, I want to log in to the application, so I can create posts.
	- Logging in happens through Google account
	- After logging, user is taken to main window after a successful logging
	- Internet connection required for Google synchronization
	- No extra forms, just an option to log in through Google account
	- User stays logged in for an indefinite amount of time, even after closing the app
3. As a logged in user, I want to log out of the application, to log in to another account.
	- User clicks on their profile to see the button to log out
	- After successfully logging out, user is taken to log in/register window
	- Internet connection required for Google synchronization
4. As a logged in user, I want to see my profile, so I can know how other people see me.
	- User is taken to their profile screen after clicking their profile picture in a corner of the screen
	- Display nickname, profile picture, some statistics, an option to log out, and customize the profile
	- Internet connection not required, this portion can be stored offline
5. As a logged in user, I want to delete my profile, so I can get on with my life.
	- User can do so in the options accessed from the profile view
	- Confirmation to do so after pressing the delete account button
	- User's account's data is then wiped and Google synchronization is removed
6. As a logged in user, I want to minimize the application, so I can use another application.
	- Freeze the current state of the application, except for things such as timer during game
	- After opening the application again, it quickly refreshes to catch up with state of game
7. As a logged in user, I want to take a picture of the street I'm currently on and post it, so others can guess and earn points.
	- User has the option to press a button which takes him to his camera, take photo and show them a confirmation screen before posting the photo
	- User cannot use their gallery, they must use the in-game camera option
	- User allows the app to access GPS to automatically set the location of the photograph
	- User can dismiss the taken photo and try again to take another one
	- User can exit this screen and go back to main menu
	- User confirms whether the GPS location is right
	- User can slightly adjust the location if GPS was off
	- Internet connection required
8. As a logged in user, I want to guess where somebody else's photo was taken, so I can gain points and have fun.
	- Main menu has a button that takes them to the game mode that user can open
	- User is taken to a game
	- The app during the game shows the user a photograph of a place, and button to open map
	- The map opens, and user can drag a waypoint that's fixed to the middle of the screen
	- User then can choose to guess the location after placying the waypoint
	- Application checks the accuracy of the guess and calculates points to award the user
	- Application goes to another location to guess, if there are no more locations to guess, user goes back to the end screen
	- End screen shows points gained, names of the locations, time passed and a button to go back to main menu
	- Internet connection required
9. As a logged in user, I want to rack up as many points as possible by guessing locations, so I can feel good about myself.
	- Points are awarded after each guess in a game
	- Show total points awarded and average points per game on the user's profile and average points of the user's friends on main screen(?)
	- A leaderboard of all registered users in the application and a placement for the user
10. As a logged in user, I want to see other people's profiles, so I can know how well they're doing.
	- User can click other people's profile picture to be taken to their profiles
	- Show another user's profile details: their profile picture and nickname
	- Show total points earned, total games played, total guesses taken, average points gained per game and number of friends
	- User can send a friend request through a button on another user's profile
11. As a logged in user, I want to be able to add people to my friends list and compete with them, so I can bond with them.
	- User can search people through their friends list
	- User is then presented with a number of matching profiles they can open
	- User can add people by pressing a corresponding button on  another person's profile
12. As a logged in user, I want to be able to remove people off my friends list, so I can have less people in my friends list.
	- User can open a profile through their friends list to be taken to the desired profile
	- User can click a button to remove the person from their friends list if they're already friends
	- After doing so, the person no longer is friends with user, the profile refreshes to update information
13. As a logged in user, I want to view my friends list, so I can know exactly whom I added.
	- The button for that is going to be somewhere on the screen
	- It's going to be another window user can be taken to
	- Alternatively, it slides out of the side
	- Internet connection is required to update status of friends, otherwise show stored information without their activity status
14. As a logged in user, I want to see amounts of points of my friends, so I can compare them to each other.
	- User can sort by alphabet or by average points per game (the value is not going to be shown on that screen, user is going to have to click on the person's profile to see the value)
15. As an administrator, I want to verify other people's posts, so it's fun to use the application for others.
	- Administrator has a screen for pending posts waiting to be verified(?)
	- Administrator has a screen with all the posts of other users, that the administrator can open and be presented with options to remove the submission and to ban the poster
16. As an administrator, I want to remove posts of others', so the application stays clean.
	- Read above
17. As an administrator, I want to ban other users for inappropriate behaviour or other, so the application stays clean.
	- Read above

#### Mockups which the UI was based on
https://imgur.com/a/josxyFK