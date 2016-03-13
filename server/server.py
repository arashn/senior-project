import SocketServer
import time
import googlemaps
from polyline.codec import PolylineCodec
from dronekit import connect, VehicleMode, Command
from pymavlink import mavutil

class MyTCPHandler(SocketServer.BaseRequestHandler):
    """
    The request handler class for our server.

    It is instantiated once per connection to the server, and must
    override the handle() method to implement communication to the
    client.
    """

    def handle(self):
        # self.request is the TCP socket connected to the client
        self.data = self.request.recv(1024).strip()
        print "{} wrote:".format(self.client_address[0])
        print self.data
        locations = self.data.split(';')
        if locations[0] == "nav":
            user_location = locations[1]
            destination = locations[2]
            drone_location = str(vehicle.location.global_relative_frame.lat) + "," + str(vehicle.location.global_relative_frame.lon)
            directions = get_directions(drone_location, drone_location, waypoints=[user_location, destination])
            create_mission(directions)
#            start_mission()
            print "Ready to fly"
            self.request.sendall("guideOK")
        elif locations[0] == "land":
            vehicle.mode = VehicleMode("LAND")
            self.request.sendall("landOK")

gmaps = googlemaps.Client(key='AIzaSyBj8RNUHUSuk78N2Jim9yrMAKjWvh6gc_g')
vehicle = connect('/dev/ttyUSB0', baud=57600, wait_ready=True)
print "Drone is ready"

def get_directions(start_location, end_location, waypoints=None):
    directions_result = gmaps.directions(start_location, end_location, mode="walking", waypoints=waypoints)
    print directions_result
    directions = []
    for leg in directions_result[0]['legs']:
        path = []
        start = leg['steps'][0]['start_location']
        path.append((start['lat'], start['lng']))
        for step in leg['steps']:
            poly = PolylineCodec().decode(step['polyline']['points'])
            for point in poly:
                path.append(point)
            end = step['end_location']
            path.append((end['lat'], end['lng']))
        directions.append(path)
#    start = directions_result[0]['legs'][0]['steps'][0]['start_location']
#    directions.append((start['lat'], start['lng']))
#    for step in directions_result[0]['legs'][0]['steps']:
#        poly = PolylineCodec().decode(step['polyline']['points'])
#        for point in poly:
#            directions.append(point)
#        end = step['end_location']
#        directions.append((end['lat'], end['lng']))
#    for x in directions:
#        print x
    print "Coordinates:"
    for leg in directions:
        for coordinate in leg:
            print coordinate
        print
    return directions

def create_mission(directions):
    # Get the mission from the drone
    cmds = vehicle.commands

    # Clear any old waypoints from the previous mission
    cmds.clear()

    # Add a takeoff command with an altitude of 5m
    cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_TAKEOFF, 0, 0, 0, 0, 0, 0, 0, 0, 5))

    # Add waypoints for the path from the base location to the user
    for point in directions[0]:
        lat = float(point[0])
        lon = float(point[1])

        cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_WAYPOINT, 0, 0, 1, 0, 0, 0, lat, lon, 5))

    # Add a command to loiter at the user's location for 10 seconds at 5m
    cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_LOITER_TIME, 0, 0, 10, 0, 0, 0, 0, 0, 5))

    # Add waypoints for the path from the user to the destination
    # Don't add the last waypoint to avoid getting too close to any buildings
    for point in directions[1][:-3]:
        lat = float(point[0])
        lon = float(point[1])

        cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_WAYPOINT, 0, 0, 1, 0, 0, 0, lat, lon, 5))

    # Add a command to loiter at the destination for 10 seconds at 5m
    cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_LOITER_TIME, 0, 0, 10, 0, 0, 0, 0, 0, 5))

    # Add waypoints for the path from the destination to the base location
    # Don't add the first waypoint to avoid getting too close to any buidings
    for point in directions[2][1:]:
        lat = float(point[0])
        lon = float(point[1])

        cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_WAYPOINT, 0, 0, 1, 0, 0, 0, lat, lon, 5))

    # Add a command to land at the base location
    cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_LAND, 0, 0, 0, 0, 0, 0, 0, 0, 0))

    # Upload the mission to the drone
    cmds.upload()

def start_mission():
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

if __name__ == "__main__":
    HOST, PORT = "", 9999

    # Create the server, binding to localhost on port 9999
    server = SocketServer.TCPServer((HOST, PORT), MyTCPHandler)

    # Activate the server; this will keep running until you
    # interrupt the program with Ctrl-C
    server.serve_forever()

