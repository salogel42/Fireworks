# Fireworks
A game of logic, memory, and careful information sharing.

## State of the app
The game currently works (only tested with 2 players so far), but does not have any persistent data
(i.e. switching apps will utterly kill it at this point, not to mention closing the app).


### Missing features (things I hope to implement):
* persistent data 
  * saving state at all
  * saving state to long term storage so you can come back to a game later even after closing the app
  * storing game logs?
* allow users to rearrange cards
* notifications when it's your turn if the app isn't active (for Words With Friends style play)
* automatic marking of known info (possibly with settings for how far to take this)
* allow user marking of known info (privately, based on other people's hands, or as an alternative to auto marking)
* build an AI (or multiple different ones?) so users can play solo (or remove the single player button if not!)

## How to install

Currently, you'll need to build from source to get the most up to date version.  I have created a signed APK for the 
version as of the writing of this readme, and it's available here: https://github.com/salogel42/Fireworks/blob/master/app/app-debug.apk

You will need an APK installer on your android device to be able to install this; I use one that is literally called
"Apk Installer" which seems to work pretty well.

Feel free to report issues, unless they are about the above known missing features.
