from time import sleep
import math
import pigpio
from queue import Queue
import threading


class Move:

    def __init__(self, totalDelay, mainWaveRepetition, leadRampUp, leadMain, leadRampDown, followRampUp, followMain, followRampDown):

        self.totalDelay = totalDelay  # in Nano sec
        self.mainWaveRepetition = mainWaveRepetition
        self.leadRampUp = leadRampUp
        self.leadMain = leadMain
        self.leadRampDown = leadRampDown
        self.followRampUp = followRampUp
        self.followMain = followMain
        self.followRampDown = followRampDown


class MotionDataProcessThread(threading.Thread):

    def __init__(self, threadID, dataQueue, motion):
        threading.Thread.__init__(self)
        self.threadID = threadID
        self.dataQueue = dataQueue
        self.motion = motion
        self.quit = False

        return

    def run(self):

        while not self.quit:

            if self.dataQueue.empty():

                sleep(2)
            else:
                self.motion.addMove(self.dataQueue.get())

    def stop(self):

        self.quit = True


class Motion:

    def __init__(self, pi, robot):

        self.pi = pi
        self.robot = robot

        self.SPEED = 1500  # steps/s
        self.INITIAL_SPEED = 200  # steps/s

        self.ACCELERATION = 1000  # steps/s/s

        self.TIMER_FREQUENCY = 1000000

        self.LEADER_MOTOR_RAMP_DISTANCE = (
            self.SPEED ** 2 - self.INITIAL_SPEED ** 2) / (2 * self.ACCELERATION)

        self.INITIAL_DELAY = self.TIMER_FREQUENCY / \
            (self.INITIAL_SPEED ** 2 + 2 * self.ACCELERATION) ** (1/2)

        self.NANO_DELAY = self.TIMER_FREQUENCY / self.SPEED

        self.CONSTANT_MULTIPLIER = self.ACCELERATION / self.TIMER_FREQUENCY ** 2

        self.MAX_PULSES = 5500  # max pulses smaller because of memory

        self.MAX_CBS = self.pi.wave_get_max_cbs()

        self.MAX_RAMP_UP_STEPS = 2000

        self.MAX_STEPS = self.MAX_PULSES / 2 - self.MAX_RAMP_UP_STEPS

        self.moves = Queue()

        self.pi.wave_clear()

    def addMove(self, move_parts):
        """
        Adds a wave to the motion wave list


        """

        # motor1 is leader motor
        motor1, motor2 = sorted(move_parts, key=lambda x: x.delayPerStep)

        stepsMotor1 = motor1.rotations * motor1.stepper.stepsPerRevolution
        stepsMotor2 = motor2.rotations * motor2.stepper.stepsPerRevolution

        if stepsMotor1 == 0:

            self.moves.put(
                Move(
                    0,
                    0,
                    [],
                    [],
                    [],
                    [],
                    [],
                    []
                )
            )
        else:

            leaderMotorRampDistance = self.LEADER_MOTOR_RAMP_DISTANCE

            if stepsMotor1 <= leaderMotorRampDistance * 2:

                leaderMotorRampDistance = stepsMotor1 / 2

            leaderMotorRampStepRatio = leaderMotorRampDistance / stepsMotor1

            leaderMotorMainWaveRatio = (
                stepsMotor1 - leaderMotorRampDistance * 2) / stepsMotor1

            leadMotorRampUpPulses, leadMotorRampTotalDelay = self.getPulses(

                steps=leaderMotorRampDistance,
                movePart=motor1,
                initialDelay=self.INITIAL_DELAY,
                targetDelay=motor1.delayPerStep,
                acceleration=-1

            )

            leadMotorRampDownPulses = list(reversed(leadMotorRampUpPulses))

            if round(stepsMotor2) == 0:

                followMotorRampUpPulses = []
                followMotorMainWavePulses = []
                followMotorRampDownPulses = []

            else:

                followMotorRampUpSteps = int(
                    round(stepsMotor2 * leaderMotorRampStepRatio))

                followMotorAcceleration = 0 if followMotorRampUpSteps == 0 else ((leadMotorRampTotalDelay -
                                                                                  followMotorRampUpSteps *
                                                                                  motor2.delayPerStep) / sum(range(1, followMotorRampUpSteps + 1)))

                followMotorInitialDelay = motor2.delayPerStep + \
                    followMotorAcceleration * followMotorRampUpSteps

                followMotorRampUpPulses, followMotorRampTotalDelay = self.getPulses(

                    steps=followMotorRampUpSteps,
                    movePart=motor2,
                    initialDelay=followMotorInitialDelay,
                    targetDelay=motor2.delayPerStep,
                    acceleration=-followMotorAcceleration,
                    leaderMotor=False
                )

                followMotorRampDownPulses = list(
                    reversed(followMotorRampUpPulses))

            mainStepsTotal = (stepsMotor1 * leaderMotorMainWaveRatio,
                              stepsMotor2 * leaderMotorMainWaveRatio)

            stepsTotal = mainStepsTotal[0] + mainStepsTotal[1]

            mainWaveRepetition = math.ceil(stepsTotal / self.MAX_STEPS)

            leadMotorMainWavePulses, t1 = self.getPulses(

                steps=0 if mainWaveRepetition == 0 else mainStepsTotal[0] /
                mainWaveRepetition,
                movePart=motor1,
                initialDelay=motor1.delayPerStep,
                targetDelay=motor1.delayPerStep,
                acceleration=0

            )

            followMotorMainWavePulses, t2 = self.getPulses(

                steps=0 if mainWaveRepetition == 0 else mainStepsTotal[1] /
                mainWaveRepetition,
                movePart=motor2,
                initialDelay=motor2.delayPerStep,
                targetDelay=motor2.delayPerStep,
                acceleration=0

            )

            self.moves.put(Move(
                t1 * mainWaveRepetition + leadMotorRampTotalDelay * 2,
                mainWaveRepetition,
                leadMotorRampUpPulses,
                leadMotorMainWavePulses,
                leadMotorRampDownPulses,
                followMotorRampUpPulses,
                followMotorMainWavePulses,
                followMotorRampDownPulses)
            )

        # adds instructions for pi wave_chain.

    def getPulses(self, steps, movePart, initialDelay, targetDelay, acceleration=0, leaderMotor=True):
        """
            :param steps: int number of steps
            :param movePart: movePart object
            :param initialDelay: delay of the first step
            :param acceleration: value including (-1, 0, 1) if leaderMotor

        """

        pulses = []
        delay = initialDelay

        totalDelay = 0

        if steps <= 0:
            return pulses, 0

        pulses.append(pigpio.pulse((1 << movePart.stepper.dir) if movePart.dir == 1 else 0,
                                   0 if movePart.dir == 1 else (1 << movePart.stepper.dir), 1))
        for i in range(0, int(round(steps))):

            totalDelay += delay

            halfDelay = int(round(delay / 2))
            pulses.append(pigpio.pulse(
                1 << movePart.stepper.step, 0, halfDelay))
            pulses.append(pigpio.pulse(
                0, 1 << movePart.stepper.step, halfDelay))

            delay = max(delay + (
                (delay * (self.CONSTANT_MULTIPLIER * acceleration * delay * delay)) if leaderMotor == True
                else acceleration
            ), targetDelay)

        return pulses, totalDelay

    def execNextMove(self, sleeping):

        wid = []

        try:

            move = self.moves.get()

            if not move.totalDelay == 0 and not sleeping:

                if len(move.followRampUp) > 0 or len(move.leadRampUp) > 0:
                    self.pi.wave_add_generic(move.followRampUp)
                    self.pi.wave_add_generic(move.leadRampUp)

                    wid.append(self.pi.wave_create())

                if len(move.followMain) > 0 or len(move.leadMain) > 0:
                    self.pi.wave_add_generic(move.followMain)
                    self.pi.wave_add_generic(move.leadMain)

                    wid.append(self.pi.wave_create())

                if len(move.followRampDown) > 0 or len(move.leadRampDown) > 0:
                    self.pi.wave_add_generic(move.followRampDown)
                    self.pi.wave_add_generic(move.leadRampDown)

                    wid.append(self.pi.wave_create())

                if len(wid) == 3:
                    self.pi.wave_chain(
                        [wid[0], 255, 0, wid[1], 255, 1, move.mainWaveRepetition, 0, wid[2]])
                elif len(wid) == 2:
                    self.pi.wave_chain([wid[0], wid[1]])
                elif len(wid) == 1:
                    self.pi.wave_chain([wid[0]])

                sleep(move.totalDelay/1000000.0)

        except KeyboardInterrupt:
            self.robot.stop()
            raise

        self.pi.wave_clear()
