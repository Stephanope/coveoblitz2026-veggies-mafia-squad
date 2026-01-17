package codes.blitz.game.bot;

import codes.blitz.game.generated.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Bot {
  Random random = new Random();

  public Bot() {
    System.out.println("Initializing your super mega duper bot");
  }

  /*
   * Here is where the magic happens, for now the moves are not very good. I bet you can do better ;)
   */
  public List<Action> getActions(TeamGameState gameMessage) {
    List<Action> actions = new ArrayList<>();

    TeamInfo myTeam = gameMessage.world().teamInfos().get(gameMessage.yourTeamId());
    if (myTeam.spawners().isEmpty()) {
      actions.add(new SporeCreateSpawnerAction(myTeam.spores().getFirst().id()));
    } else if (myTeam.spores().isEmpty()) {
      actions.add(new SpawnerProduceSporeAction(myTeam.spawners().getFirst().id(), 20));
    } else {
      actions.add(
          new SporeMoveToAction(
              myTeam.spores().getFirst().id(),
              new Position(
                  random.nextInt(gameMessage.world().map().width()),
                  random.nextInt(gameMessage.world().map().height()))));
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

}
