'''
This is a simple Python server which uses the Google Maps Directions API
to obtain directions between two locations. It takes two locations as input,
a start location and an end location. Locations are represented by tuples,
with the first element being the latitude and the second element being
the longitude. It will return a list of tuples, which represent coordinates
on the path from the start location to the end location.
'''
import socketserver
import googlemaps

class MyTCPHandler(socketserver.BaseRequestHandler):
	"""
	The RequestHandler class for our server.

	It is instantiated once per connection to the server, and must
	override the handle() method to implement communication to the
	client.
	"""

	def handle(self):
		# self.request is the TCP socket connected to the client
		self.data = self.request.recv(1024).strip()
		print("{} wrote:".format(self.client_address[0]))
		print(self.data)
		locations = str(self.data, "utf-8").split(';')
		for x in locations:
			print(x)
		# just send back the same data, but upper-cased
		#self.request.sendall(self.data.upper())
		get_directions(locations[0], locations[1])

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
	HOST, PORT = "", 9999

	# Create the server, binding to localhost on port 9999
	server = socketserver.TCPServer((HOST, PORT), MyTCPHandler)

	# Activate the server; this will keep running until you
	# interrupt the program with Ctrl-C
	server.serve_forever()

#get_directions("33.648333,-117.841282", "33.645930,-117.846119")
