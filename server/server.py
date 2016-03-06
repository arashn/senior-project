import SocketServer
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
        start_location = locations[0]
        end_location = locations[1]
        directions = get_directions(start_location, end_location)
        create_mission(directions)
        # just send back the same data, but upper-cased
        self.request.sendall("Guide is on the way")

gmaps = googlemaps.Client(key='AIzaSyBj8RNUHUSuk78N2Jim9yrMAKjWvh6gc_g')
vehicle = connect('/dev/ttyUSB0', baud=57600, wait_ready=True)
print "Drone is ready"

def get_directions(start_location, end_location):
    directions_result = gmaps.directions(start_location, end_location, mode="walking")
    print directions_result
    print "Coordinates:"
    directions = []
    start = directions_result[0]['legs'][0]['steps'][0]['start_location']
    directions.append((start['lat'], start['lng']))
    for step in directions_result[0]['legs'][0]['steps']:
        poly = PolylineCodec().decode(step['polyline']['points'])
        for point in poly:
            directions.append(point)
        end = step['end_location']
        directions.append((end['lat'], end['lng']))
    for x in directions:
        print x
    return directions

def create_mission(directions):
    cmds = vehicle.commands

    cmds.clear()

    cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_TAKEOFF, 0, 0, 0, 0, 0, 0, 0, 0, 5))

    for point in directions:
        lat = float(point[0])
        lon = float(point[1])

        cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_WAYPOINT, 0, 0, 1, 0, 0, 0, lat, lon, 5))

    cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_LOITER_TIME, 0, 0, 10, 0, 0, 0, 0, 0, 5))
    cmds.add(Command(0, 0, 0, mavutil.mavlink.MAV_FRAME_GLOBAL_RELATIVE_ALT, mavutil.mavlink.MAV_CMD_NAV_LAND, 0, 0, 0, 0, 0, 0, 0, 0, 0))

    cmds.upload()

if __name__ == "__main__":
    HOST, PORT = "", 9999

    # Create the server, binding to localhost on port 9999
    server = SocketServer.TCPServer((HOST, PORT), MyTCPHandler)

    # Activate the server; this will keep running until you
    # interrupt the program with Ctrl-C
    server.serve_forever()

