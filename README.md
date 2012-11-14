Sikuli Extension Guide
======================

this is the latest available version running with Sikuli X-1.0rc3+

The license is the same as for Sikuli X.

**BE AWARE:** at time of coming into existence late 2011, this was work in progress. 
So there might be any bug or inconvenience. 
<br />Feel free, to post a comment here. I will try to help you.

Usage out of the box
--------------------

- download guide.jar
- in a script use

load("absolute-path-to-guide.jar")<br />
import guide

There is currently no actual documentation 
<br />(the chapter in the docs is outdated, since it is related to the version of Guide from the time of X-1.0rc2).

For information about features and their usage have a look into **guide.py** here https://github.com/RaiMan/SikuliGuide2012/blob/master/src/guide/guide.py.

Working with the sources
------------------------

Feel free to make any changes to the sources.

Setup a java project in Netbeans or Eclipse using the src directory structure from this repo.

The libraries needed to setup a useable guide.jar can be found in the folder lib. <br />Additionally you need a valid library link to sikuli-script.jar.

Known issues
------------
- images(target, image) gives an error. add a 3rd parameter as<br />
images(target, image, side="choice")<br />
where choice might be over, right, left, top or bottom
