package codes.blitz.game.bot;

import codes.blitz.game.generated;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Bot {
  boolean initialized = false;
  Position[][] nutrientPositions;
  Random random = new Random();

  public Bot() {
    System.out.println("Initializing your super mega duper bot");
  }

  /* Here is where the magic happens, for now the moves are not very good. I bet you can do better 😉*/
  public List<Action> getActions(TeamGameState gameMessage) 
  {
    if (!initialized) {
      nutrientPositions = getNutrientPositions(gameMessage.world().map());
      initialized = true;
    // }

    List<Action> actions = new ArrayList<>();
    myTeamId = gameMessage.yourTeamId();
    Spores mySpores = gameMessage.world().teams().get(myTeamId).spores();
    Spawners mySpawners = gameMessage.world().teams().get(myTeamId).spawners();

    for (int i = 0; i < mySpores.size(); i++) {
      Spores spore = mySpores.get(i);
      if (spore.biomass() == spawnerCost(mySpawners.size()) || (spore.biomass() == 1 && mySpawners.size() == 0)) {
        actions.add(new SporeCreateSpawnerAction(spore.id()));
      }      
    }

    // You can clearly do better than the random actions above. Have fun!!
    return actions;
  }

  public Position[] getNutrientPositions(GameMap map) {
    Position[] positions = null;
    for (int i= 0; i < map.width(); i++){
      for (int j= 0; j < map.height(); j++){
        positions.add(new Position(i,j));
        System.out.println("Nutrient found at " + i + "," + j);
      }
    }

    return positions;
  }

  public int spawnerCost(int spawnersOnMap) {
    return (int) Math.pow(2, spawnersOnMap) - 1;
  }


}