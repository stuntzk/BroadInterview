// This is my main class that runs my program
public class MBTA {
  public static void main(String[] args) {
    RouteInformation routeInfo = new RouteInformation();
    routeInfo.retrieveRouteNames();
    routeInfo.processStops();
    // I did not finish implementing the answer for question three. I wrote the bulk of implementation
    // but did not do anything to implement how the user can enter the stops they want. To do this
    // I would have just used a scanner and done it in the console.
  }
}