

import math

class Vec:
    """docstring for Vec."""


    def __init__(self, x, y):

        self.x = x
        self.y = y


    def subtract(self, vec):

        return Vec(self.x - vec.x, self.y - vec.y)

    def getLength(self):

        return math.hypot(self.x,self.y)
