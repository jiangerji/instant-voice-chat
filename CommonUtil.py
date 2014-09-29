#encoding=utf-8

from twisted.python import log

import random
import struct
_chars = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"
_len = len(_chars)

def RandomName(length):
    result = ""
    if length > 0:
        for i in range(length):
            result += _chars[random.randint(0, _len-1)]

    return result

def SendCmd(cmdType, content, client):
    if content == None:
        content = ""
    log.msg("Send cmd: %08x, %20s"%(cmdType, content[0:min(20, len(content))]))
    format = "2I%ds"%len(content)
    sendContent = struct.pack(format, cmdType, len(content), content);
    log.msg("Send length: %d"%len(sendContent))
    client.transport.write(sendContent)