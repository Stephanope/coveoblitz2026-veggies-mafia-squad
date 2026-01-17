package codes.blitz.game.bot;

import codes.blitz.game.generated.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Bot {
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
  /*
   * Here is where the magic happens, for now the moves are not very good. I bet you can do better ;)
   */
  public List<Action> getActions(TeamGameState gameMessage) {
    List<Action> actions = new ArrayList<>();
    if (!initialized && gameMessage.yourTeamId()!= null) {
      teamId = gameMessage.yourTeamId();
      nutrientPositions = getNutrients(gameMessage.world().map());
      initialized = true;
  }
    TeamInfo myTeam = gameMessage.world().teamInfos().get(gameMessage.yourTeamId());
    if (myTeam.spawners().isEmpty()) {
      actions.add(new SporeCreateSpawnerAction(myTeam.spores().getFirst().id()));
    } else if (myTeam.spores().isEmpty()) {
      actions.add(new SpawnerProduceSporeAction(myTeam.spawners().getFirst().id(), 20));
    } else {
      
       walkToClosestNutriment(myTeam, gameMessage.world(), actions);
    }

    // You can clearly do better than the random actions above. Have fun!!
    return actions;
  }

  public boolean isSpawnerInDanger(Spawner spawner, GameWorld world, TeamGameState gameMessage) {
    String myTeamId = gameMessage.yourTeamId();
    int spawnerX = spawner.position().x();
    int spawnerY = spawner.position().y();

    for (Spore potentialEnemy : world.spores()) {
        if (!potentialEnemy.teamId().equals(myTeamId)) {
            
            if (potentialEnemy.biomass() < 2) {
                continue; 
            }

            int enemyX = potentialEnemy.position().x();
            int enemyY = potentialEnemy.position().y();
            
            int distance = Math.abs(enemyX - spawnerX) + Math.abs(enemyY - spawnerY);

            if (distance <= 3) {
                System.out.println("ALERTE");
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
        if (!otherSpore.teamId().equals(myTeamId)) {
            
            int otherX = otherSpore.position().x();
            int otherY = otherSpore.position().y();

            int distance = Math.abs(otherX - myX) + Math.abs(otherY - myY);

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
  public void walkToClosestNutriment(TeamInfo myTeam, GameWorld world, List<Action> actions) {
    // TODO implement
    for ( Spore spore: myTeam.spores()) {
      Position nutrientPosition = findClosestNutriment(spore, world);
      //System.out.println("Spore at position: " + sporePos);
      System.out.println(" nutriend Id: " + world.ownershipGrid()[nutrientPosition.x()][nutrientPosition.y()]);
      if(nutrientPosition == null) {
       actions.add(new SporeMoveAction(
              spore.id(),
              new Position(0, 1))); 
       }else{      
        actions.add(new SporeMoveToAction(
              spore.id(),
              nutrientPosition));
      }
      
    }
    
  }
  public Position findClosestNutriment(Spore spore, GameWorld world) {
    Position sporePos = spore.position();
    Position bestPos = null;

    double minDistance = Double.MAX_VALUE;
     System.out.println("Team ID: " + teamId);
    for (Nutrient nutrient : nutrientPositions) {
      
      if(!getNutrientOwnership(world, nutrient.position()).equals(teamId)){
        Position target = nutrient.position();
        int distance = Math.abs(sporePos.x() - target.x()) 
                     + Math.abs(sporePos.y() - target.y());
        if (distance < minDistance) {
            minDistance = distance;
            bestPos = target;
        }
    }  
  }
    return bestPos; 
  }
  String getNutrientOwnership(GameWorld world, Position pos) {
    return world.ownershipGrid()[pos.x()][pos.y()];
  }
}


