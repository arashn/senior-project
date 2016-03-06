# This is a simple script to start the vehicle's mission
# without input from the remote control
from dronekit import connect, VehicleMode
from pymavlink import mavutil

vehicle = connect('/dev/ttyUSB0', baud=57600, wait_ready=True)
print "Vehicle ready"

start = vehicle.message_factory.command_long_encode(0, 0, mavutil.mavlink.MAV_CMD_MISSION_START, 0, 0, 0, 0, 0, 0, 0, 0)

while not vehicle.is_armable:
    print " Waiting for vehicle to initialise..."
    time.sleep(1)

print "Arming motors"
vehicle.mode = VehicleMode("GUIDED")
vehicle.armed = True

while not vehicle.armed:
    print " Waiting for arming..."
    time.sleep(1)

print "Starting mission!"
vehicle.mode = VehicleMode("AUTO")
vehicle.send_mavlink(start)
