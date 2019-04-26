#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
#  main.py
from time import sleep
from blueComm import BlueComm
from typer import TyperDataProcessThread
import math
from typer import Typer
import ptvsd
from queue import Queue
import threading

instructionsQueue = Queue()


def messageCallback(blueComm, data):

    msg = data.decode("utf-8")

    if msg == "q":

        blueComm.stop = True

    elif msg == "c":

        blueComm.stop = True
        blueComm.exit = True

    else:

        # print(msg)
        instructions = []

        for item in msg.split(sep=","):

            p = item.split(sep=" ")

            if len(p) == 1:

                try:
                    instructions.append(int(p[0]))
                except ValueError:
                    instructions.append(p[0])

            elif len(p) == 2:
                instructions.append((float(p[0]), float(p[1])))

        instructionsQueue.put(instructions)


class TyperDirectInputProcessThread(threading.Thread):

    def __init__(self, threadID, typer, blueComm):
        threading.Thread.__init__(self)
        self.threadID = threadID
        self.quit = False
        self.typer = typer
        self.blueComm = blueComm

        return

    def run(self):

        while not self.quit:

            messageCallback(self.blueComm, input(
                "Enter Typer sequence:").encode())

    def stop(self):
        self.quit = True
# test


def main(args):
    # ptvsd.enable_attach('secret')
    # ptvsd.wait_for_attach()

    # typer= Typer();

    # typer.stop()

    exit = False

    workers = []

    while not exit:
        try:

            typer = Typer()

            blueComm = BlueComm()

            workers.append(TyperDataProcessThread(1, instructionsQueue, typer))
            workers.append(TyperDirectInputProcessThread(2, typer, blueComm))

            for worker in workers:
                worker.start()

            blueComm.start(messageCallback)

            exit = blueComm.exit

        except KeyboardInterrupt:
            print("STOP")

            break

    for worker in workers:
        worker.stop()
        worker.join()

    return 0


if __name__ == '__main__':
    import sys

    sys.exit(main(sys.argv))
