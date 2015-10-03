echo off
cls
:start
echo Building SaveyBot...
javac src\SaveyBot.java src\Main.java -cp ".;src;libs\pircbot-1.5.0\pircbot.jar"
echo SaveyBot built! Would you like to run? [y/n]
SET /P saveybotyn=[y/n] 
if /i "%saveybotyn%"=="y" goto runprogram
goto end
:runprogram
SET /P serversaveybot=[server url] 
SET /P channelsaveybot=[channel (incl '#')] 
java -cp ".;src;libs\pircbot-1.5.0\pircbot.jar" Main %serversaveybot% %channelsaveybot%
echo FINISHED.
echo Would you like to rebuild SaveyBot?
SET /P saveybotyn=[y/n]
if /i "%saveybotyn%"=="y" goto start
echo Exiting...
:end