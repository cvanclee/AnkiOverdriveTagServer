Deployment:
Both the client and server require Java 8 to be installed. The client requires connection to 
a network that can communicate with the server, and vice versa. The server should work on any 
Linux machine with Bluetooth Low Energy capabilities. 

The general procedure is to make sure the server's Bluetooth adapter is working, then 
installing NodeJS and Java 8. Lastly, if the included node modules found in 
"AnkiOverdriveTagServer/dist/nodejs/node_modules/" are not compatible with your machine, 
delete them and install the required modules using the node package manager. The required 
modules are at the top of the "AnkiOverdriveTagServer/dist/nodejs/server.js" file.

The following instructions are the step-by-step server installation instructions, to be 
performed on a clean installation, and only performed on an identical or very similar setup
to the machine detailed at the end of this document.

1: (Optional) If doing a clean installation, recommendeded installation instructions here:
https://www.raspberrypi.org/documentation/installation/noobs.md
2: Boot Pi
3: Establish an internet connection if not already connected.
(https://raspberrypi.stackexchange.com/a/79238 may help for fresh installs at universities)
4: Open a terminal
5: Enter 'sudo apt-get update' and then 'sudo apt-get upgrade'
6: Reboot
7: Open a terminal, enter 'sudo apt-get install nodejs' and then 'sudo apt-get install npm'
8: cd to where you want the server files to reside, enter 
'git clone https://github.com/cvanclee/AnkiOverdriveTagServer'. (you may have to delete the 
node_modules folder contents and use npm to build the modules if on a different system)
9: Run the server with 'java -jar AnkiOverdriveTagServer/dist/AnkiOverdriveTagServer.jar'
	
Installation instructions and server tested on:
	Machine: Raspberry Pi 3 Model B+
	Machine firmware: Linux raspberrypi 4.14.69-v7+, armv7l
	OS: Raspbian GNU/Linux 9 (stretch)
	Java version: 1.8.0_65
	NodeJS version: v8.12.0
