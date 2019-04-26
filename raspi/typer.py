import collections
from motion import MotionDataProcessThread
from builtins import tuple
from builtins import type

import math
import pigpio
from time import sleep
from types import *

import json

from motion import Motion
from vec import Vec
from motors import Stepper, Servo
import threading
from queue import Queue
import keyboard
import curses


class MovePart:

    def __init__(self, stepper, direction, rotations, delayPerStep):
        self.stepper = stepper
        self.dir = direction
        self.rotations = rotations
        self.delayPerStep = delayPerStep  # in nano sec


class TyperDataProcessThread(threading.Thread):

    def __init__(self, threadID, dataQueue, typer):
        threading.Thread.__init__(self)
        self.threadID = threadID
        self.dataQueue = dataQueue
        self.quit = False
        self.typer = typer

        return

    def run(self):

        while not self.quit:

            if self.dataQueue.empty():

                sleep(2)
            else:

                self.typer.execSequence(self.dataQueue.get())

    def stop(self):

        self.typer.stop()

        self.quit = True


class Typer:
    WIDTH = 500  # mm
    HEIGHT = 200  # mm
    PER_REVOLUTION_MOVEMENT = 40  # mm
    SQRT_OF_TWO = math.sqrt(2)

    PEN_INPUT = 23
    X_MIN = 14
    Y_MIN = 15

    k = PER_REVOLUTION_MOVEMENT / SQRT_OF_TWO

    def cbf(self, gpio, level, tick):

        if not self.drawingMode:
            if tick - self.lastTick > 125000:

                self.lastTick = tick

                sleep(0.03)
                self.penDown(False)

    def xMinDown(self, gpio, level, tick):

        if self.goingHome and self.xMin and not self.sleeping:

            self.sleeping = True

            for stepper in self.motors.steppers:

                stepper.sleep(True)

    def yMinDown(self, gpio, level, tick):

        if self.goingHome and self.yMin and not self.sleeping:

            self.sleeping = True

            for stepper in self.motors.steppers:

                stepper.sleep(True)

    def onKeyDown(self, event):

        self.keyPositions["k" + str(event.scan_code)] = (
            self.lastPos.x,
            self.lastPos.y
        )

        print(self.keyPositions)

    def __init__(self):

        self.pi = pigpio.pi()

        self.c = 0
        self.drawingMode = False

        # pen input
        self.pi.set_mode(Typer.PEN_INPUT, pigpio.INPUT)
        self.pi.set_pull_up_down(Typer.PEN_INPUT, pigpio.PUD_UP)

        self.pi.callback(Typer.PEN_INPUT, pigpio.FALLING_EDGE, self.cbf)

        # x and y mins
        self.pi.set_mode(Typer.X_MIN, pigpio.INPUT)
        self.pi.set_pull_up_down(Typer.PEN_INPUT, pigpio.PUD_UP)

        self.pi.set_mode(Typer.Y_MIN, pigpio.INPUT)
        self.pi.set_pull_up_down(Typer.PEN_INPUT, pigpio.PUD_UP)

        self.pi.callback(Typer.Y_MIN, pigpio.RISING_EDGE, self.yMinDown)

        self.pi.callback(Typer.X_MIN, pigpio.RISING_EDGE, self.xMinDown)

        self.xMin = False
        self.yMin = False

        self.goingHome = False
        self.sleeping = False

        #---

        try:

            keyboard.on_press(self.onKeyDown)

        except AttributeError:

            print("no keyboard detected")

        self.keyPositions = {'k7': (112.0, 78.0), 'k3': (35.0, 78.0), 'k46': (80.0, 19.0), 'k45': (60.0, 17.0), 'k38': (184.0, 48.0), 'k23': (160.0, 60.0), 'k25': (200.0, 65.0), 'k48': (117.0, 24.0), 'k6': (95.0, 78.0), 'k20': (100.0, 57.0), 'k8': (132.0, 78.0), 'k51': (175.0, 25.0), 'k31': (50.0, 40.0), 'k9': (152.0, 78.0), 'k52': (195.0, 25.0), 'k28': (250.0, 47.0), 'k36': (145.0, 41.0), 'k37': (165.0, 45.0), 'k10': (170.0, 78.0), 'k15': (
            1.0, 52.0), 'k4': (55.0, 78.0), 'k33': (90.0, 40.0), 'k29': (0.0, 1.0), 'k2': (15.0, 75.0), 'k42': (10.0, 17.0), 'k16': (25.0, 55.0), 'k17': (45.0, 60.0), 'k30': (30.0, 40.0), 'k50': (157.0, 21.0), 'k32': (70.0, 40.0), 'k18': (65.0, 65.0), 'k19': (80.0, 60.0), 'k34': (107.0, 40.0), 'k49': (137.0, 21.0), 'k24': (179.0, 60.0), 'k35': (127.0, 43.0), 'k22': (140.0, 60.0), 'k11': (186.0, 79.0), 'k5': (75.0, 78.0), 'k57': (100.0, 1.0), 'k58': (0.0, 35.0)}

        self.pos = Vec(0, 0)
        self.lastPos = self.pos

        self.up = True
        self.seq = Queue()
        self.lastTick = 0

        self.motion = Motion(self.pi, self)

        self.motionWorker = MotionDataProcessThread(2, Queue(), self.motion)
        self.motionWorker.start()

        Motors = collections.namedtuple("Motors", "steppers, servos")

        self.motors = Motors(
            steppers=(
                Stepper(self.pi, 200, 1 / 2, 20, 21, 12, 16),
                Stepper(self.pi, 200, 1 / 2, 7, 24, 25, 8)
            ),
            servos=(Servo(self.pi),)

        )

        self.goHome()

    def getMotorRatio(self, motorRate):

        h = math.hypot(motorRate.x, motorRate.y)

        angle = math.atan2(motorRate.y, motorRate.x) + math.pi / 4

        return Vec(math.cos(angle) * h, math.sin(angle) * h)

    def wakeUp(self):
        self.sleeping = False

        for stepper in self.motors.steppers:
            stepper.sleep(False)

        sleep(0.001)

    def goHome(self):

        self.goingHome = True
        self.setHome()

        s = []
        print('x')
        for i in range(-5, -550, -5):

            s.append((self.pos.x + i, 0))

        self.xMin = True

        if self.pi.read(Typer.X_MIN):

            self.xMinDown(1, 1, 1)

        self.execSequence(s)
        self.xMin = False

        self.wakeUp()
        self.setHome()

        s = []
        print('y')
        for i in range(-5, -250, -5):

            s.append((0, self.pos.y + i))

        self.yMin = True

        if self.pi.read(Typer.Y_MIN):

            self.yMinDown(1, 1, 1)

        self.execSequence(s)
        self.yMin = False

        self.wakeUp()
        self.setHome()

    def setHome(self):

        self.pos = Vec(0, 0)

    def penDown(self, down):

        if bool(down):

            if not self.drawingMode:
                self.motors.servos[0].setAngle(180)

                self.up = False

            else:
                self.motors.servos[0].setAngle(80)
                sleep(0.3)

                self.nextStep()

        else:

            if not self.up or self.drawingMode:

                self.c += 1
                self.up = True
                self.motors.servos[0].setAngle(20)
                sleep(0.13)
                self.motors.servos[0].stop()
                self.nextStep()

    def execSequence(self, seq):

        if len(seq) < 1:
            pass

        for i in range(0, len(seq)):

            if type(seq[i]) is str:

                if seq[i] in self.keyPositions:

                    seq[i] = self.keyPositions[seq[i]]

                    self.seq.put(seq[i])

            else:

                self.seq.put(seq[i])

        self.queueMoves(filter(lambda x: type(x) is tuple, seq))

        self.nextStep()

    def nextStep(self):

        if not self.seq.empty():

            instruction = self.seq.get()

            if type(instruction) is tuple:

                self.lastPos = Vec(instruction[0], instruction[1])
                self.motion.execNextMove(self.sleeping)

                self.nextStep()

            else:

                if instruction == 0 or instruction == 1:

                    self.penDown(instruction)
                elif instruction == 2:
                    self.setHome()
                    self.nextStep()
                elif instruction == 3:
                    self.goHome()
                    self.nextStep()

    def queueMoves(self, seq):

        for pos in seq:

            newPosition = Vec(pos[0], pos[1])
            moveVec = newPosition.subtract(self.pos)

            moveSlope = moveVec.y / (moveVec.x or 0.0000000001)

            speedSlope = (math.copysign(1, moveVec.x) *
                          math.copysign(1, moveVec.y) * -1) or 1
            speedYintercept = (Typer.SQRT_OF_TWO *
                               math.copysign(1, moveVec.y)) or Typer.SQRT_OF_TWO

            motorRate = Vec(0, 0)

            motorRate.x = speedYintercept / (moveSlope - speedSlope)
            motorRate.y = speedSlope * motorRate.x + speedYintercept

            motorSpeed = Vec(motorRate.x * Typer.k, motorRate.y * Typer.k)
            motorRatio = self.getMotorRatio(motorRate)

            rotations = moveVec.getLength() / motorSpeed.getLength()

            self.motionWorker.dataQueue.put(
                (
                    MovePart(
                        stepper=self.motors.steppers[0],
                        direction=int(math.copysign(1, motorRatio.x) < 0),
                        rotations=rotations * abs(motorRatio.x),
                        delayPerStep=self.motion.NANO_DELAY /
                            (abs(motorRatio.x) or 1)
                    ),

                    MovePart(
                        stepper=self.motors.steppers[1],
                        direction=int(math.copysign(1, motorRatio.y) < 0),
                        rotations=rotations * abs(motorRatio.y),
                        delayPerStep=self.motion.NANO_DELAY /
                            (abs(motorRatio.y) or 1)
                    )
                )
            )

            self.pos = newPosition

    def stop(self):

        self.motionWorker.stop()

        self.motionWorker.join()

        for motorType in self.motors:

            for motor in motorType:
                motor.stop()

        self.pi.stop()

        keyboard.unhook_all()
