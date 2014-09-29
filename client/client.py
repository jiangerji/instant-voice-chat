# encoding=utf-8
import sys
sys.path.append("..")

import struct
import time

from twisted.internet import reactor, protocol
from twisted.python import log

from CommonUtil import *
from Constants import *


myClient = None


import threading
class CmdThread(threading.Thread):
    def __init__(self, client):
        threading.Thread.__init__(self)
        self.client = client

    def run(self):
        while 1:
            cmdType = raw_input("input cmd:")
            cmdType = int(cmdType, 16)
            if cmdType == SPEAKING_START:
                self.client.startSpeaking()
            elif cmdType == SPEAKING_STOP:
                self.client.stopSpeaking()

# a client protocol

class EchoClient(protocol.Protocol):
    global myClient
    """Once connected, send a message, then print the result."""

    def __init__(self):
        self.previousData = ""
        self.speakingFile = None

    def connectionMade(self):
        myClient = self
        CmdThread(self).start()
    
    def dataReceived(self, data):
        if data != None:
            log.msg("Data received: %d"%len(data))

            self.previousData += data
            while len(self.previousData) >= 8:
                cmdType, contentLength = struct.unpack("2I", self.previousData[0:8])
                if not self.handleData(cmdType, contentLength):
                    break

    def handleData(self, cmdType, contentLength):
        cmdLength = contentLength + 8
        if len(self.previousData) >= cmdLength:
            format = "2I%ds"%contentLength
            cmdType, contentLength, content = struct.unpack(format, self.previousData[0:cmdLength])
            # 数据足够处理一条命令
            log.msg("Handle cmd: 0x%08x %d"%(cmdType, cmdLength))
            if cmdType == SPEAKING_CONTENT:
                if self.speakingFile != None:
                    self.speakingFile.write(content)
            elif cmdType == SPEAKING_START:
                self.speakingFile = open(".speakingFile_"+RandomName(4), "wb")
            elif cmdType == SPEAKING_STOP:
                self.speakingFile.close()

            self.previousData = self.previousData[contentLength+8:]
            return True
        else:
            return False
    
    def connectionLost(self, reason):
        log.msg("connection lost")
        myClient = None

    def startSpeaking(self):
        cmdType = SPEAKING_START
        content = "start speaking"
        self.sendCmd(cmdType, content)

        self.sendSpeakingContent()

    def sendSpeakingContent(self):
        fp = open("1.pcm", "rb")
        bufferSize = 100*1024
        content = fp.read()
        index = 0
        while index < len(content):
            self.sendCmd(SPEAKING_CONTENT, content[index:min(index+bufferSize, len(content))])
            index += bufferSize

        fp.close()
        self.stopSpeaking()

    def stopSpeaking(self):
        cmdType = SPEAKING_STOP
        content = "stop speaking"
        self.sendCmd(cmdType, content)

    def sendCmd(self, cmdType, content):
        if content == None:
            content = ""

        SendCmd(cmdType, content, self)

class EchoFactory(protocol.ClientFactory):
    protocol = EchoClient

    def clientConnectionFailed(self, connector, reason):
        log.msg("Connection failed - goodbye!")
        reactor.stop()
    
    def clientConnectionLost(self, connector, reason):
        log.msg("Connection lost - goodbye!")
        reactor.stop()


# this connects the protocol to a server runing on port 8000
def main():
    print dir(EchoFactory)
    f = EchoFactory()
    reactor.connectTCP("localhost", 9999, f)
    reactor.run()

# this only runs if the module was *not* imported
if __name__ == '__main__':
    Release = True
    if Release:
        logFile = "log.txt"
        if len(sys.argv) > 1:
            logFile = "log_"+RandomName(8)+".txt"
        log.startLogging(open(logFile, "w"))
        main()
    else:
        fp = open("1", "rb")
        fp2 = open("2", "wb")
        bufferSize = 10*1024
        content = fp.read()
        index = 0
        while index < len(content):
            fp2.write(content[index:min(index+bufferSize, len(content))])
            index += bufferSize
