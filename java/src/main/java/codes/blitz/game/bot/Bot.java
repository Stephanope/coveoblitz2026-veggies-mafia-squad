package codes.blitz.game.bot;

import codes.blitz.game.generated.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
  public void walkToClosestNutriment(TeamInfo myTeam, GameWorld world, List<Action> actions) {
    // TODO implement
    for ( Spore spore: myTeam.spores()) {
      Position nutrientPosition = findClosestNutriment(spore, world);
      if(nutrientPosition == null) {
       moveToClosestNonOwnedTile(world, spore, actions);
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
      
      if(!getTileOwnership(world, nutrient.position()).equals(teamId)){
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
  String getTileOwnership(GameWorld world, Position pos) {
    return world.ownershipGrid()[pos.x()][pos.y()];
  }
  Position nextPos(int x, int y, Position pos) {
    return new Position(x + pos.x(), y + pos.y());
  }
 
  void moveToClosestNonOwnedTile(GameWorld world, Spore spore, List<Action> actions) {
    Position currentPos = spore.position();
    for (Position dir : DIRECTIONS) {
        int targetX = currentPos.x() + dir.x();
        int targetY = currentPos.y() + dir.y();
        if (targetX >= 0 && targetX < world.map().width() && 
            targetY >= 0 && targetY < world.map().height()) {
            String tileOwner =getTileOwnership(world, new Position(targetX, targetY));
            if (!tileOwner.equals(teamId)) {
              System.out.println("Moving spore " + spore.id() + " to non-owned tile at (" + targetX + ", " + targetY + ")");
                actions.add(new SporeMoveAction(spore.id(), dir));
                return;
            }
        }
    }
    Position randomDir = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
    actions.add(new SporeMoveAction(spore.id(), randomDir));
}
}


