#encoding=utf-8
import sys
sys.path.append("..")

from twisted.python import log

from CommonUtil import RandomName, SendCmd
from Constants import *

class Room:
    ROOMS = {}

    def __init__(self):
        self.name = RandomName(16)
        while Room.ROOMS.has_key(self.name):
            self.name = RandomName(16)

        Room.ROOMS[self.name] = self

        self.clients = []
        self.speakingClient = None

    def __del__(self):
        Room.ROOMS.pop(self.name, None)

    def leave(self):
        Room.ROOMS.pop(self.name, None)

    def addClient(self, client):
        self.clients.append(client)

    def removeClient(self, client):
        self.clients.remove(client)

    def startSpeaking(self, client):
        self.speakingClient = client
        for _client in self.clients:
            if id(_client) != id(client):
                SendCmd(SPEAKING_START, "Someone start speaking!", _client)

    def stopSpeaking(self, client):
        for _client in self.clients:
            if id(_client) != id(self.speakingClient):
                SendCmd(SPEAKING_STOP, "Someone stop speaking!", _client)
        self.speakingClient = None

    def speaking(self, content):
        for _client in self.clients:
            if id(_client) != id(self.speakingClient):
                SendCmd(SPEAKING_CONTENT, content, _client)
