package codes.blitz.game.bot;

import codes.blitz.game.generated.*;
import java.util.*;

public class Bot {

  private final Position[] DIRECTIONS = {
    new Position(0, -1), // Haut
    new Position(0, 1),  // Bas
    new Position(-1, 0), // Gauche
    new Position(1, 0)   // Droite
};

  Random random = new Random();
  boolean initialized = false;
  List<Nutrient> nutrientPositions;
  String teamId;

  public Bot() {
    System.out.println("Initializing your super mega duper bot");
  }

public List<Nutrient> getNutrients(GameMap map) {
        List<Nutrient> nutrients = new ArrayList<>();
        var grid = map.nutrientGrid(); 
        for (int i = 0; i < map.width(); i++) {
            for (int j = 0; j < map.height(); j++) {
                int value = grid[i][j]; 
                if (value > 0) {
                    nutrients.add(new Nutrient(new Position(i, j), value));
                }
            }
        }
        return nutrients;
    }
    
    public List<Action> getActions(TeamGameState gameMessage) {
        List<Action> actions = new ArrayList<>();

        if (!initialized) {
            teamId = gameMessage.yourTeamId();
            nutrientPositions = getNutrients(gameMessage.world().map());
            initialized = true;
        }

        

    String myTeamId = gameMessage.yourTeamId();
    List<Spore> mySpores = gameMessage.world().teamInfos().get(myTeamId).spores();
    List<Spawner> mySpawners = gameMessage.world().teamInfos().get(myTeamId).spawners();
    int myNutrients = gameMessage.world().teamInfos().get(myTeamId).nutrients();
    TeamInfo myTeam = gameMessage.world().teamInfos().get(teamId);

  // SPAWNERS ON NUTRIENTS
    for (int i = 0; i < mySpores.size(); i++) {
    Spore spore = mySpores.get(i);
  
    for(int j = 0; j < nutrientPositions.size(); j++) {
        
        if (getDistanceFromSpawner(spore.position(), gameMessage.world()) > 5 && myNutrients > 1) {
            if (spawnerCost(mySpawners.size()) <= spore.biomass()) {
              actions.add(new SporeCreateSpawnerAction(spore.id()));
            }
              break; 
      }
     }

    }

    int totalStrength = 0;
    for(int i = 0; i < mySpores.size(); i++) {
        totalStrength += mySpores.get(i).biomass();
    }
    
     // EARLY GAME
     if (mySpawners.size() > 0) {
        if (myNutrients <= 150) {
            if (totalStrength < 50 && mySpores.size() < 6) {
                actions.add(new SpawnerProduceSporeAction(myTeam.spawners().getLast().id(), 10));
            }
        }
        
        // MID GAME
        else if (myNutrients > 150 && myNutrients < 1000) {
                 if (totalStrength < 250 && mySpores.size() < 12) {
                    actions.add(new SpawnerProduceSporeAction(myTeam.spawners().getLast().id(), myNutrients / 5));

            }
        }
        
        // END GAME
        else if (myNutrients >= 1000) {
            actions.add(new SpawnerProduceSporeAction(myTeam.spawners().getLast().id(), myNutrients));
        }
    }


    // IF ELSE INITAL POUR LE DEBUT DE PARTIE
        if (myTeam.spawners().isEmpty() && !myTeam.spores().isEmpty()) {
            actions.add(new SporeCreateSpawnerAction(myTeam.spores().getLast().id()));
        } else if (myTeam.spawners().isEmpty() && myTeam.spores().isEmpty()) {
        } else if (myTeam.spores().isEmpty()) {
        actions.add(new SpawnerProduceSporeAction(myTeam.spawners().getLast().id(), myNutrients / 5));
        } else {
            walkToClosestNutriment(myTeam, gameMessage, actions);
            
            for (Spawner spawner : myTeam.spawners()) {
                if (isSpawnerInDanger(spawner, gameMessage.world(), gameMessage, actions)) {
                    if (myTeam.nutrients() >= 20) {
                        actions.add(new SpawnerProduceSporeAction(spawner.id(), myNutrients / 5));
                    }
                }
            }
        }

        return actions;
    }

    public int getDistanceFromSpawner(Position pos1, GameWorld world) {
        int minDistance = Integer.MAX_VALUE;
        for (Spawner spawner : world.spawners()) {
            if (spawner.teamId() != null && spawner.teamId().equals(teamId)) {
                int d = Math.abs(spawner.position().x() - pos1.x()) + Math.abs(spawner.position().y() - pos1.y());
                if (d < minDistance) {
                    minDistance = d;
                }
            }
        }
        return minDistance == Integer.MAX_VALUE ? Integer.MAX_VALUE : minDistance;
    }

    public Position getNextStepDijkstra(Spore spore, Position target, GameWorld world, String neutralTeamId) {
        Position start = spore.position();
        int width = world.map().width();
        int height = world.map().height();
        String myTeamId = spore.teamId();
        int myBiomass = spore.biomass();

        Spore[][] sporeMap = new Spore[width][height];
        for (Spore s : world.spores()) {
            sporeMap[s.position().x()][s.position().y()] = s;
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.cost));
        Map<String, Double> costSoFar = new HashMap<>();
        
        openSet.add(new Node(start, 0, null));
        costSoFar.put(start.x() + "," + start.y(), 0.0);
        
        Node finalNode = null;
        int iterations = 0; 
        int maxIterations = 1000; 

        while (!openSet.isEmpty() && iterations < maxIterations) {
            Node current = openSet.poll();
            iterations++;

            if (current.position.x() == target.x() && current.position.y() == target.y()) {
                finalNode = current;
                break;
            }

            int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
            
            for (int[] dir : directions) {
                int newX = current.position.x() + dir[0];
                int newY = current.position.y() + dir[1];

                if (newX < 0 || newX >= width || newY < 0 || newY >= height) continue;

                Position nextPos = new Position(newX, newY);
                double stepCost = 1.0; 

                Spore occupant = sporeMap[newX][newY];
                if (occupant != null) {
                    if (occupant.teamId().equals(neutralTeamId)) {
                        stepCost = 9999; 
                    } else if (!occupant.teamId().equals(myTeamId)) {
                        if (myBiomass > occupant.biomass() + 1) {
                            stepCost = 1.0; 
                        } else {
                            stepCost = 9999; 
                        }
                    }
                }

                if (stepCost < 100) { 
                     String tileOwner = world.ownershipGrid()[newX][newY];
                     if (tileOwner != null && tileOwner.equals(myTeamId)) {
                         stepCost = 0; 
                     }
                }

                double newCost = current.cost + stepCost;
                String key = newX + "," + newY;

                if (!costSoFar.containsKey(key) || newCost < costSoFar.get(key)) {
                    costSoFar.put(key, newCost);
                    double priority = newCost + (Math.abs(target.x() - newX) + Math.abs(target.y() - newY)); 
                    openSet.add(new Node(nextPos, priority, current)); 
                }
            }
        }

        if (finalNode != null) {
            Node curr = finalNode;
            while (curr.parent != null && !curr.parent.position.equals(start)) {
                curr = curr.parent;
            }
            return curr.position; 
        }
        System.out.println("Dijkstra: No path found from " + start + " to " + target);
        return null; 
    }


  public void walkToClosestNutriment(TeamInfo myTeam, TeamGameState gameMessage, List<Action> actions) {
        GameWorld world = gameMessage.world();
        String neutralTeamId = gameMessage.constants().neutralTeamId();
        List<Nutrient> tempNutrients = new ArrayList<>(nutrientPositions);
        for (Spore spore : myTeam.spores()) {
            Position targetNutrient = findClosestNutriment(spore, world, tempNutrients);
            if (targetNutrient != null) {
                Position nextStep = getNextStepDijkstra(spore, targetNutrient, world, neutralTeamId);
                

                if (nextStep != null) {
                    actions.add(new SporeMoveToAction(spore.id(), nextStep));
                } 
            }else {
                    moveToClosestNonOwnedTile(world, spore, actions);
                }
        }
    }
  
  String getTileOwnership(GameWorld world, Position pos) {
    return world.ownershipGrid()[pos.x()][pos.y()];
  }
  Position nextPos(int x, int y, Position pos) {
    return new Position(x + pos.x(), y + pos.y());
  }
 
 void moveToClosestNonOwnedTile(GameWorld world, Spore spore, List<Action> actions) {
    System.out.println("No nutriment found for spore " + spore.id() + ", searching for non-owned tile.");
    int width = world.map().width();
    int height = world.map().height();
    Position startPos = spore.position();
    Queue<SearchNode> queue = new LinkedList<>();

    boolean[][] visited = new boolean[width][height];

    visited[startPos.x()][startPos.y()] = true;
    for (Position dir : DIRECTIONS) {
        int nx = startPos.x() + dir.x();
        int ny = startPos.y() + dir.y();

        if (isValid(nx, ny, width, height)) {

            queue.add(new SearchNode(new Position(nx, ny), dir));
            visited[nx][ny] = true;
        }
    }
    while (!queue.isEmpty()) {
        SearchNode current = queue.poll();
        Position currentPos = current.pos;
        String tileOwner = getTileOwnership(world, currentPos);
        if (tileOwner == null || !tileOwner.equals(teamId)) {

            actions.add(new SporeMoveToAction(spore.id(), currentPos));
            return;
        }
        for (Position dir : DIRECTIONS) {
            int nx = currentPos.x() + dir.x();
            int ny = currentPos.y() + dir.y();

            if (isValid(nx, ny, width, height) && !visited[nx][ny]) {
                visited[nx][ny] = true;
                queue.add(new SearchNode(new Position(nx, ny), current.initialDirection));
            }
        }
    }

    Position randomDir = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
    actions.add(new SporeMoveAction(spore.id(), randomDir));
}

private record SearchNode(Position pos, Position initialDirection) {}

private boolean isValid(int x, int y, int width, int height) {
    return x >= 0 && x < width && y >= 0 && y < height;
}

    public Position findClosestNutriment(Spore spore, GameWorld world, List<Nutrient> nutrientToHunt) {
        Position sporePos = spore.position();
        Position bestPos = null;
        double minDistance = Double.MAX_VALUE;
        int bestIndex = -1;

        for (int i = 0; i < nutrientToHunt.size(); i++) {
            Nutrient nutrient = nutrientToHunt.get(i);
            String owner = getTileOwnership(world, nutrient.position());
            boolean isMyNutrient = owner != null && owner.equals(teamId);
            
            if (!isMyNutrient) { 
                Position target = nutrient.position();
                int distance = Math.abs(sporePos.x() - target.x()) + Math.abs(sporePos.y() - target.y());
                
                if (distance < minDistance) {
                    minDistance = distance;
                    bestPos = target;
                    bestIndex = i;
                }
            }
        }
        if(bestIndex >= 0) {
           nutrientToHunt.remove(bestIndex);
        } 
        return bestPos;
    }

    public int spawnerCost(int spawnersOnMap) {
    return (int) Math.pow(2, spawnersOnMap) - 1;
  }

    public boolean isSpawnerInDanger(Spawner spawner, GameWorld world, TeamGameState gameMessage, List<Action> actions) {
        String myTeamId = gameMessage.yourTeamId();
        int spawnerX = spawner.position().x();
        int spawnerY = spawner.position().y();
        int myNutrients = gameMessage.world().teamInfos().get(myTeamId).nutrients();

        for (Spore potentialEnemy : world.spores()) {
            if (!potentialEnemy.teamId().equals(myTeamId) && !potentialEnemy.teamId().equals(gameMessage.constants().neutralTeamId())) {
                if (potentialEnemy.biomass() < 2) {
                    continue; 
                }
                int distance = Math.abs(potentialEnemy.position().x() - spawnerX) + Math.abs(potentialEnemy.position().y() - spawnerY);
                if (distance <= 3) {
                   if (distance <= 3) {
                    
                   if (potentialEnemy.biomass()< myNutrients / 5 && !(getAllyLevel(potentialEnemy, world, gameMessage) > potentialEnemy.biomass())) {
                     actions.add(new SpawnerProduceSporeAction(spawner.id(), myNutrients / 5));
                   }
                    
                   }
                    return true; 
                }
            }
        }
        return false;
    }
    
    public int getThreatLevel(Spore mySpore, GameWorld world, TeamGameState gameMessage) {
        String myTeamId = gameMessage.yourTeamId();
        int myX = mySpore.position().x();
        int myY = mySpore.position().y();
        int maxEnemyBiomass = 0;
    
        for (Spore otherSpore : world.spores()) {
            if (!otherSpore.teamId().equals(myTeamId) && !otherSpore.teamId().equals(gameMessage.constants().neutralTeamId())) {
                int distance = Math.abs(otherSpore.position().x() - myX) + Math.abs(otherSpore.position().y() - myY);
                if (distance <= 3) {
                    if (otherSpore.biomass() > maxEnemyBiomass) {
                        maxEnemyBiomass = otherSpore.biomass();
                    }
                }
            }
        }
        return maxEnemyBiomass;
    }
    public int getAllyLevel(Spore mySpore, GameWorld world, TeamGameState gameMessage) {
        String myTeamId = gameMessage.yourTeamId();
        int myX = mySpore.position().x();
        int myY = mySpore.position().y();
        int maxEnemyBiomass = 0;
    
        for (Spore otherSpore : world.spores()) {
            if (otherSpore.teamId().equals(myTeamId)) {
                int distance = Math.abs(otherSpore.position().x() - myX) + Math.abs(otherSpore.position().y() - myY);
                if (distance <= 3) {
                    if (otherSpore.biomass() > maxEnemyBiomass) {
                        maxEnemyBiomass = otherSpore.biomass();
                    }
                }
            }
        }
        return maxEnemyBiomass;
    }

    public List<Position> getMyNutrientTiles(GameWorld world, String myTeamId) {
        List<Position> richTiles = new ArrayList<>();
        int width = world.map().width();
        int height = world.map().height();
        int[][] nutrientGrid = world.map().nutrientGrid();
        String[][] ownershipGrid = world.ownershipGrid();
    
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                String tileOwner = ownershipGrid[x][y];
                int nutrients = nutrientGrid[x][y];
                if (tileOwner != null && tileOwner.equals(myTeamId) && nutrients >= 1) {
                    richTiles.add(new Position(x, y));
                }
            }
        }
        return richTiles;
    }

   

    private static class Node {
        Position position;
        double cost;
        Node parent;

        Node(Position position, double cost, Node parent) {
            this.position = position;
            this.cost = cost;
            this.parent = parent;
        }
    }
 }
    