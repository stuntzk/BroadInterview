import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

// I chose to use the Jackson library to help parse the json objects. I found that this made it
// much simpler and relatively clear what I was doing. I did not use Maven or Gradle as that
// felt excessive for this assignment and instead downloaded the jar files and used them as
// external dependencies
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// This class has all the information to complete the programming task. It is responsible
// for sending the requests to the API as well as processing it and printing out the information
// necessary to complete this task.
public class RouteInformation {

  // routeIds represents the different route ids and the long names associated with them
  private final HashMap<String, String> routeIds;

  // allStops represents every stop in route types 0 or 1 and the list of routes that go through
  // that stop
  private final HashMap<String, ArrayList<String>> allStops;

  // lineConnections represent each route and the routes that they connect with
  private final HashMap<String, HashSet<String>> lineConnections;
  private final ObjectMapper objectMapper = new ObjectMapper();
  public RouteInformation() {
    this.routeIds = new HashMap<>();
    this.allStops = new HashMap<>();
    this.lineConnections = new HashMap<>();
  }

  public RouteInformation(HashMap<String, String> routeIds, HashMap<String, ArrayList<String>> allStops, HashMap<String, HashSet<String>> lineConnections) {
    this.routeIds = routeIds;
    this.allStops = allStops;
    this.lineConnections = lineConnections;
  }

  /*
  This function prints out the list of long names for each route necessary to complete question1
   */
  public void retrieveRouteNames() {
    HttpResponse<String> response = null;
    try {
      // I chose to filter based on route type in the request rather than manually because it made
      // my code more condense and cleaner. As well as it did not take any more time than just
      // requesting the routes and filtering them manually. They could still be customized later
      // on by having this function take in a parameter of the route types wanted and including
      // them all in the URL.
      response = this.sendRequest("https://api-v3.mbta.com/routes?filter[type]=0,1");
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

    try {
      JsonNode lineArr = objectMapper.readTree(response.body()).get("data");
      System.out.println(processRouteNames(lineArr));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  /*
  The purpose of this function is to take in the HttpResponse with all the different routes
  and compile a list of all of their names. It also adds in the routeIds with the long
  names to streamline the process for getting all the stop calculations later on in the
  program
   */
  public String processRouteNames (JsonNode lineArr) throws JsonProcessingException {
    ArrayList<String> allLines = new ArrayList<>();
    if (lineArr.isArray()) {
      // grabs the long name for each route returned by the request
      for (JsonNode line : lineArr) {
        JsonNode attributes = line.get("attributes");
        String longName = attributes.get("long_name").asText();
        allLines.add(longName);

        // Puts each route into the routeId map
        String id = line.get("id").asText();
        routeIds.put(id, longName);
      }
    }
    return formatListProperly(allLines);
  }

  /*
  The purpose of this function is to send the HttpRequest requesting information wanted to the
  API and then returning the result of it. It returns the whole result and not just the body of it
   */
  private HttpResponse<String> sendRequest(String url) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .build();
    HttpResponse<String> response = null;
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }


  public void processStops() {
    // this represents the number of stops that each route has
    HashMap<Integer, ArrayList<String>> routeStops = new HashMap<>();

    routeIds.forEach((lineId, lineName) -> {
      HttpResponse<String> response = null;
      try {
        String url = "https://api-v3.mbta.com/stops?filter[route]=" + lineId;
        response = this.sendRequest(url);
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }

      try {
        JsonNode stopArr = objectMapper.readTree(response.body()).get("data");
        this.processLineStops(lineName, stopArr, routeStops);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }


    });

    Integer max = Collections.max(routeStops.keySet());
    ArrayList<String> maxLines = routeStops.get(max);
    System.out.println("Maximum stops: " + this.formatListProperly(maxLines) + " - " + max.toString());
    Integer min = Collections.min(routeStops.keySet());
    ArrayList<String> minLines = routeStops.get(min);
    System.out.println("Minimum stops: " + this.formatListProperly(minLines) + " - " + min.toString());


    ArrayList<String> multipleStopLines = new ArrayList<>();
    allStops.forEach((stop, lines) -> {
      if (lines.size() > 1) {
        multipleStopLines.add(String.format(stop + " - " + this.formatListProperly(lines)));

        // record which lines connect to help with question three
        for (String line : lines) {
          if (lineConnections.containsKey(line)) {
            lineConnections.get(line).addAll(lines);
          } else {
            HashSet<String> linesAsSet = new HashSet<>(lines);
            lineConnections.put(line,linesAsSet);
          }
        }
      }
    });

    // removes the self reference from the lineConnections map
    lineConnections.forEach((line, connections) -> {
      connections.remove(line);
    });

    System.out.println("Stops with multiple routes: " + this.formatListProperly(multipleStopLines));
  }

  /*
  This function goes through the array of stops for each route and records the count in the routeStops map
   */
  public void processLineStops(String lineName, JsonNode stopArr, HashMap<Integer, ArrayList<String>> routeStops) {
    HashSet<String> stops = new HashSet<>();
    if (stopArr.isArray()) {
      for (JsonNode stop : stopArr) {
        JsonNode attributes = stop.get("attributes");
        String stopName = attributes.get("name").asText();
        stops.add(stopName);
        if (allStops.containsKey(stopName)) {
          allStops.get(stopName).add(lineName);
        } else {
          ArrayList<String> stopLines = new ArrayList<>();
          stopLines.add(lineName);
          allStops.put(stopName, stopLines);
        }
      }
    }
    ArrayList<String> currentStops = routeStops.putIfAbsent(stops.size(), new ArrayList<>(List.of(lineName)));
    if (currentStops != null) {
      currentStops.add(lineName);
    }
  }

  private String formatListProperly(ArrayList<String> list) {
    return list.toString().replace("[", "").replace("]", "");
  }

  // this function is used to find the route to get from stop1 to stop2. It is not fully implemented
  // however details that would be further implemented are included in comments within the code
  private String findConnection(String stop1, String stop2) {
    ArrayList<String> stop1Lines = allStops.get(stop1);
    ArrayList<String> stop2Lines = allStops.get(stop2);

    // checks to see if they are on any of the same lines together
    for (String line : stop1Lines) {
      if (stop2Lines.contains(line)) {
        return line;
      }
    }

    // if they are not on any of the same lines then the connections map from earlier is used
    for (String line : stop1Lines) {
      String[] connections = new String[lineConnections.get(line).size()];
      connections = lineConnections.get(line).toArray(connections);

      for (String connectingLine : connections) {
        if (stop2Lines.contains(connectingLine)) {
          return line + ", " + connectingLine;
        }
      }
    }

    // Due to time constrictions, I stopped here. At this current moment this will only work to
    // connect stops that you only need to switch once to get to. In the future I would go on to
    // go through the connections of the second layer line and look for them in the stop2 lines.
    // I would probably update my code to follow a recursive structure/a while loop to make it
    // easier and have less repetitive code.

    return "No such connection";
  }
}