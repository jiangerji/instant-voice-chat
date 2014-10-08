#encoding=utf-8

import sys
sys.path.append("..")

import struct

from twisted.internet.protocol import ServerFactory
from twisted.protocols.basic import LineReceiver
from twisted.python import log
from twisted.internet import reactor
from twisted.internet import protocol

from room import Room
from Constants import *

class Echo(protocol.Protocol):

    def __init__(self):
        self.client_ip = None
        self.speakingFile = None
        self.previousData = ""

    def connectionMade(self):
        self.client_ip = self.transport.getPeer()
        log.msg("Client connection from %s" % self.client_ip)
        if len(self.factory.clients) >= self.factory.clients_max:
            log.msg("Too many connections. bye !")
            self.client_ip = None
            self.transport.loseConnection()
        else:
            # self.factory.clients.append(self)
            self.factory.addClient(self)

    def connectionLost(self, reason):
        log.msg('Lost client connection.  Reason: %s' % reason)
        # self.factory.clients.remove(self)
        self.factory.removeClient(self)

    def dataReceived(self, data):
        if data != None:
            log.msg("Data received: %d"%len(data))

            self.previousData += data
            while len(self.previousData) >= 8:
                cmdType, contentLength = struct.unpack("2I", self.previousData[0:8])
                # format = "2I%ds"%contentLength
                # cmdType, contentLength, content = struct.unpack(format, data[0:8+contentLength])
                # log.msg("Data received: %s"%content)
                if not self.handleData(cmdType, contentLength):
                    break

    def handleData(self, cmdType, contentLength):
        cmdLength = contentLength + 8
        log.msg("Handle cmd: 0x%08x %d"%(cmdType, cmdLength))
        if len(self.previousData) >= cmdLength:
            format = "2I%ds"%contentLength
            cmdType, contentLength, content = struct.unpack(format, self.previousData[0:cmdLength])
            # 数据足够处理一条命令
            if cmdType == SPEAKING_CONTENT:
                if self.speakingFile != None:
                    self.speakingFile.write(content)
                self.factory.speaking(content)
            elif cmdType == SPEAKING_START:
                self.speakingFile = open(".speakingFile", "w")
                # 发送有人说话消息到其他客户端
                self.factory.startSpeaking(self)
            elif cmdType == SPEAKING_STOP:
                self.speakingFile.close()
                # 发送说话结束消息到其他客户端
                self.factory.stopSpeaking(self)

            self.previousData = self.previousData[contentLength+8:]
            return True
        else:
            return False


class MyFactory(ServerFactory):

    protocol = Echo

    def __init__(self, clients_max=10):
        self.clients_max = clients_max
        self.clients = []
        self.room = Room()

    def startSpeaking(self, client):
        # 某个客户端开始说话
        log.msg("Client start speaking: %s"%hex(id(client)))
        self.room.startSpeaking(client)

    def speaking(self, content):
        log.msg("Client speaking: %d"%len(content))
        self.room.speaking(content)        

    def stopSpeaking(self, client):
        # 某个客户端开始说话
        log.msg("Client stop speaking: %s"%hex(id(client)))
        self.room.stopSpeaking(client)

    def addClient(self, client):
        self.clients.append(client)
        self.room.addClient(client)

    def removeClient(self, client):
        self.clients.remove(client)
        self.room.removeClient(client)

log.startLogging(sys.stdout)
reactor.listenTCP(9999, MyFactory())
reactor.run()