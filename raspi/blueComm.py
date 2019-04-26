

import ptvsd

from bluetooth import (BluetoothSocket,
                       PORT_ANY,
                       RFCOMM,
                       advertise_service,
                       SERIAL_PORT_PROFILE,
                       SERIAL_PORT_CLASS)


class BlueComm:

    def __init__(self):

        self.stop = False
        self.exit = False

    def start(self, callback):

        self.stop = False

        self.server_sock = BluetoothSocket(RFCOMM)
        self.server_sock.bind(("", PORT_ANY))
        self.server_sock.listen(1)

        port = self.server_sock.getsockname()[1]

        uuid = "fa87c0d0-afac-11de-8a39-0800200c9a66"

        advertise_service(self.server_sock, "blueCommServer",
                          service_id=uuid,
                          service_classes=[uuid, SERIAL_PORT_CLASS],
                          profiles=[SERIAL_PORT_PROFILE]
                          #  protocols = [ OBEX_UUID ]
                          )

        print("Waiting for connection on RFCOMM channel %d" % port)

        self.client_sock, self.client_info = self.server_sock.accept()
        print("Accepted connection from ", self.client_info)

        try:
            while not self.stop:
                data = self.client_sock.recv(1024)
                if len(data) == 0:
                    break

                callback(self, data)

        except IOError:
            pass

        print("disconnected")

        self.disconnect()

    def disconnect(self):

        self.client_sock.close()
        self.server_sock.close()
