

import math
import pigpio


class Motor:
    def __init__(self, pi):

        self.pi = pi


class Servo (Motor):
    def __init__(self, pi):

        super().__init__(pi)

        self.dir = 18
        pi.set_mode(self.dir, pigpio.OUTPUT)

        self.stop()

    def setAngle(self, angle):

        a = int(angle)

        if a <= 180 and a >= 0:

            self.pi.hardware_PWM(self.dir, 50, int(a / 180 * 80000 + 20000))

    def stop(self):

        self.pi.hardware_PWM(self.dir, 0, 0)


class Stepper (Motor):
    def __init__(self, pi, stepsPerRevolution, stepType, stepPin, dirPin, offPin, resetPin):

        super().__init__(pi)

        self.stepsPerRevolution = stepsPerRevolution / stepType
        self.step = stepPin
        self.dir = dirPin
        self.off = offPin
        self.reset = resetPin

        pi.set_mode(dirPin, pigpio.OUTPUT)
        pi.set_mode(offPin, pigpio.OUTPUT)
        pi.set_mode(stepPin, pigpio.OUTPUT)
        pi.set_mode(resetPin, pigpio.OUTPUT)
        pi.write(offPin, 0)

        self.sleep(False)

    def sleep(self, on):
        self.pi.write(self.reset, not on)

    def stop(self):
        self.pi.write(self.off, 1)
