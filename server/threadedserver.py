import socket
import threading
import socketserver
import googlemaps

class ThreadedTCPRequestHandler(socketserver.BaseRequestHandler):

    def handle(self):
        #data = str(self.request.recv(1024), 'ascii')
        data = self.request.recv(1024).strip()
        user_data = str(data, 'utf-8').split(';')
        bt_addr = user_data[0]
        user_location = user_data[1]
        print(bt_addr)
        print(user_location)
        drone_location = ''
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(('192.168.1.75', 8888))
        try:
            sock.sendall(bytes('GPS', 'utf-8'))
            response = sock.recv(1024).strip()
            drone_location = str(response, 'utf-8')
            print(drone_location)
        finally:
            sock.close()
        get_directions(drone_location, user_location)
        #cur_thread = threading.current_thread()
        #response = bytes("{}: {}".format(cur_thread.name, data), 'ascii')
        #self.request.sendall(response)

class ThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    pass

#def send_to_drone(ip, port, message):
#    response = ''
#    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
#    sock.connect((ip, port))
#    try:
#        sock.sendall(bytes(message, 'utf-8'))
#        drone_data = sock.recv(1024).strip()
#        response = str(drone_data, 'utf-8')
#    finally:
#        sock.close()
#    return response

gmaps = googlemaps.Client(key='AIzaSyBj8RNUHUSuk78N2Jim9yrMAKjWvh6gc_g')

def get_directions(start_location, end_location):
    directions_result = gmaps.directions(start_location, end_location, mode="walking")
    print("Start address:", directions_result[0]['legs'][0]['start_address'])
    print("Start location:", directions_result[0]['legs'][0]['start_location'])
    print("End address:", directions_result[0]['legs'][0]['end_address'])
    print("End location:", directions_result[0]['legs'][0]['end_location'])
    print("Coordinates:")
    for x in directions_result[0]['legs'][0]['steps']:
        print("Start:", x['start_location'])
        print("End:", x['end_location'])

    coordinates = list(map((lambda x: x['end_location']), directions_result[0]['legs'][0]['steps']))
    for x in coordinates:
        print(x)
    coordinates = list(map((lambda x: (x['lat'], x['lng'])), coordinates))
    for x in coordinates:
        print(x)

if __name__ == "__main__":
    # Port 0 means to select an arbitrary unused port
    #HOST, PORT = "localhost", 0
    HOST, PORT = "", 9999

    server = ThreadedTCPServer((HOST, PORT), ThreadedTCPRequestHandler)
    ip, port = server.server_address

    # Start a thread with the server -- that thread will then start one
    # more thread for each request
    server_thread = threading.Thread(target=server.serve_forever)
    # Exit the server thread when the main thread terminates
    #server_thread.daemon = True
    server_thread.start()
    print("Server loop running in thread:", server_thread.name)

    #server.shutdown()
    #server.server_close()
