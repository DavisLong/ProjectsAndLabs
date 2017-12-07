package planetwars.strategies;

import planetwars.core.*;
import planetwars.publicapi.*;
import java.util.*;
import java.lang.Math.*;

public class Better implements IStrategy{
    private HashMap<Double, Long> planetMoves;
    private int homeID;
    private IVisiblePlanet initial;
    private List<Integer> ownedPlanets;
    private List<Integer> attackPlanets;
    private HashMap<Integer, IVisiblePlanet> idToPlanet;
    private HashMap<Integer, Integer> idToHelp;
    private List<Integer> help;
    private List<Integer> neutral;
    private int move=0;




    @Override
    public void takeTurn(List<IPlanet> planets, IPlanetOperations planetOperations, Queue<IEvent> eventsToExecute) {
        boolean sent = false;
        updatePlanets(planets);
        planetMap(planets);
        updateHelp(planets);
        long num=0;
        //      updateHelpMap(planets);
/*
        System.out.println("OWNED:  " + ownedPlanets.size());
        System.out.println("ATTACK: " + attackPlanets.size());
        System.out.println("HELP:  " + help.size());
        System.out.println("NEUTRAL:  " + neutral.size());
        System.out.println("ALL:  " + idToPlanet.size());
*/

        for (Integer i : ownedPlanets) {
            List<IVisiblePlanet> ed = findEdges(planets, idToPlanet.get(i));
            long pop = idToPlanet.get(i).getPopulation();
            int len = ed.size();

            for (IVisiblePlanet plan : ed) {
                if (attackPlanets.contains(plan.getId())) {
                    long nPop = getNetPop(plan,4);
                    if (nPop <0 && -idToPlanet.get(i).getPopulation() < nPop - 5) {
                        eventsToExecute.add(planetOperations.transferPeople(idToPlanet.get(i), plan, Math.abs(nPop)));
                    }
                    pop += nPop;
                    sent = true;
                }
                if (help.contains(idToPlanet.get(i).getId()) && plan.getPopulation() > idToPlanet.get(i).getSize()/3) {
                    eventsToExecute.add(planetOperations.transferPeople(plan, idToPlanet.get(i), pop / 4));
                    pop -= (pop / 4);
                    sent = true;
                }
                if (neutral.contains(plan.getId()) && idToPlanet.get(i).getPopulation() > 3) {
                    if (plan.getHabitability() > idToPlanet.get(i).getHabitability()) {
                        num+=2;
                        eventsToExecute.add(planetOperations.transferPeople(idToPlanet.get(i), plan, 2));
                    } else if (idToPlanet.get(i).getPopulation() > 3) {
                        eventsToExecute.add(planetOperations.transferPeople(idToPlanet.get(i), plan, 1));
                        num+=1;
                    }

                }
            }

/*            if (friendlyNeighbors(planets, idToPlanet.get(i)) && !sent) {

                List<IVisiblePlanet> lst = findEdges(planets, idToPlanet.get(i));
                for (IVisiblePlanet planet : lst) {
                    if (idToHelp.containsKey(planet.getId()) && idToHelp.get(planet.getId()) < idToHelp.get(i)) {
                        System.out.println("Here");
                        eventsToExecute.add(planetOperations.transferPeople(idToPlanet.get(planet.getId()),
                                idToPlanet.get(i), 9));

                    }
                }
                    Set<IEdge> edges = idToPlanet.get(i).getEdges();
                    for(IEdge e : edges) {
                        if(idToHelp.get(i) < idToHelp.get(e.getDestinationPlanetId())) {
                            eventsToExecute.add(planetOperations.transferPeople(idToPlanet.get(i),
                                    idToPlanet.get(e.getDestinationPlanetId()), pop-1));
                            System.out.println("Here");
                        }
                    }


            } */
            if (friendlyNeighbors(planets, idToPlanet.get(i)) && !sent && pop > 15) {
                for(IVisiblePlanet pl : ed) {
                    eventsToExecute.add(planetOperations.transferPeople(idToPlanet.get(i),pl,(pop - num)));
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Strat";
    }

    @Override
    public boolean compete() {
        return false;
    }

    public List<IVisiblePlanet> findEdges(List<IPlanet> all, IVisiblePlanet planetOwn) {
        Set<IEdge> edges = planetOwn.getEdges();
        List<IVisiblePlanet> ret = new ArrayList<>();
        for(IEdge edge : edges) {
            ret.add(idToPlanet.get(edge.getDestinationPlanetId()));
        }
        return ret;
    }

    public void updatePlanets(List<IPlanet> planets) {
        ownedPlanets = new ArrayList<>();
        attackPlanets = new ArrayList<>();
        neutral = new ArrayList<>();
        for(IPlanet planet : planets) {
            if (planet instanceof IVisiblePlanet && ((IVisiblePlanet) planet).getOwner() == Owner.SELF) {
                ownedPlanets.add(planet.getId());
            } else if(planet instanceof IVisiblePlanet && ((IVisiblePlanet) planet).getOwner() == Owner.OPPONENT) {
                attackPlanets.add(planet.getId());
            } else if(planet instanceof IVisiblePlanet) {
                neutral.add(planet.getId());
            }
        }
    }

    public void updateHelp(List<IPlanet> all) {
        help = new ArrayList<>();
        for(Integer i : ownedPlanets) {
            List<IVisiblePlanet> lst = findEdges(all,idToPlanet.get(i));
            for(IVisiblePlanet plan : lst) {
                if(attackPlanets.contains(plan.getId()) && !help.contains(plan.getId())) {
                    help.add(idToPlanet.get(i).getId());
                }
            }
        }
    }

    public void updateHelpMap(List<IPlanet> all) {  // HashMap stored as <Planet ID, Help #>
        HashMap<Integer, Integer> idToHelp = new HashMap<>();

        if(help.size() == 0) {
            return;
        } else {
            for (Integer hlp : help) {
                idToHelp.put(hlp, 0);
            } // Grabs ID's in help list already

            System.out.println("COMPARING:  " + idToHelp.size() +" & " + ownedPlanets.size());

            for (Integer i : idToHelp.keySet()) {
                List<IVisiblePlanet> edge = findEdges(all, idToPlanet.get(i));

                for (IVisiblePlanet plan : edge) {  // some planet sharing an edge
                    if (ownedPlanets.contains(plan.getId()) && !idToHelp.containsKey(plan.getId())) { // new ID
                        idToHelp.put(plan.getId(), idToHelp.get(idToPlanet.get(i).getId()) + 1);
                    } else if (idToHelp.containsKey(plan.getId()) && idToHelp.get(i) > idToHelp.get(plan.getId())) {  // ID we've seen
                        idToHelp.replace(i, idToHelp.get(plan.getId()) + 1);
                        System.out.println("HERE SOMEHOW");
                    }
                }
            }

            System.out.println("---------------------------------");
            /*for(Integer i : idToHelp.keySet()){
                System.out.println("ID:  " + i+ "   HELP_LEVEL:  " + idToHelp.get(i));
            }*/
        }
    }

    private void updateHelpMapWrapper(List<IPlanet> all) {
        int count = ownedPlanets.size();
        int o =0;
        while(o<count) {
            for (Integer i : idToHelp.keySet()) {
                List<IVisiblePlanet> edge = findEdges(all, idToPlanet.get(i));

                for (IVisiblePlanet plan : edge) {  // some planet sharing an edge
                    if (ownedPlanets.contains(plan.getId()) && !idToHelp.containsKey(plan.getId())) { // new ID
                        o++;
                        idToHelp.put(plan.getId(), idToHelp.get(idToPlanet.get(i).getId()) + 1);
                    } else if (idToHelp.containsKey(plan.getId()) && idToHelp.get(i) > idToHelp.get(plan.getId())) {  // ID we've seen
                        idToHelp.replace(i, idToHelp.get(plan.getId()) + 1);
                        System.out.println("HERE SOMEHOW");
                        o++;
                    }
                }
            }
        }
    }

    public void planetMap(List<IPlanet> planets){
        idToPlanet  = new HashMap<>();
        for(IPlanet p: planets){
            if(p instanceof IVisiblePlanet) {
                idToPlanet.put(p.getId(), (IVisiblePlanet) p);
            }
        }
    }

    public IVisiblePlanet getHome(List<IPlanet> all) {
        for(IPlanet planet : all) {
            if(planet instanceof IVisiblePlanet) {
                if(((IVisiblePlanet) planet).getOwner() == Owner.SELF){
                    System.out.println("HAVE HOME");
                    return (IVisiblePlanet) planet;
                }
            }
        }
        return null;
    }

    public boolean friendlyNeighbors(List<IPlanet> planets, IVisiblePlanet plan){
        List<IVisiblePlanet> lst = findEdges(planets,plan);
        for(IVisiblePlanet planet : lst) {
            if(planet.getOwner() != Owner.SELF){
                return false;
            }
        }
        return true;
    }

    public long numFighting(List<IPlanet> planets, IVisiblePlanet planet) {
        long ret = 0;
        List<IVisiblePlanet> lst = findEdges(planets,planet);
        for(IVisiblePlanet plan : lst) {
            if(planet.getOwner() == Owner.OPPONENT){
                ret++;
            }
        }
        return ret;
    }

    public boolean nextAbove(IVisiblePlanet planet) {
        double pop = planet.getPopulation();
        double h = planet.getHabitability();
        double m = planet.getSize();
        pop = pop * (1 + (h / 100));
        return pop >= m;
    }

    public double aboveMax(IVisiblePlanet planet) {
        double pop = planet.getPopulation();
        double h = planet.getHabitability();
        double m = planet.getSize();

        pop = pop * (1 + (h / 100));
        double ret = pop - m;
        return ret;
    }

    public long getNetPop(IVisiblePlanet planet, int numTurns) {
        List<IShuttle> shuttle = planet.getIncomingShuttles();
        int turns;
        int temp = 0;
        long pop;
        long ret = 0;
        int ind=0;

        if(planet.getOwner() == Owner.SELF) {
            turns = 100;
            pop = planet.getPopulation();


            if(shuttle.size() != 0) {
                while (shuttle.size() > 0) {
                    for (int i = 0; i < shuttle.size(); i++) {
                        if (shuttle.get(i).getTurnsToArrival() < turns) {
                            turns = shuttle.get(i).getTurnsToArrival();
                            ind = i;
                        }
                    }

                    // We now have the minimum number of turns to the next arrival, need to see who owns it to proceed
                    if (shuttle.get(ind).getOwner() == Owner.SELF && shuttle.get(ind).getTurnsToArrival() <= numTurns) {
                        int h = planet.getHabitability();
                        int max = (int) planet.getSize();
                        long incoming = shuttle.get(ind).getNumberPeople();
                        for (int i = 0; i < turns - temp; i++) { // temp is a counter of how many steps have already been taken
                            pop = pop * (1 + (h / 100));
                            if (pop >= max) {
                                pop = pop - ((pop - max) * (1/10));
                            }
                        }
                        pop += incoming;
                        // subtract number of turns
                        temp += turns; // updates the counter

                    } else if(shuttle.get(ind).getTurnsToArrival() <= numTurns){
                        int h = planet.getHabitability();
                        int max = (int) planet.getSize();
                        long incoming = shuttle.get(ind).getNumberPeople();
                        for (int i = 0; i < turns - temp; i++) { // temp is a counter of how many steps have already been taken
                            pop = pop * (1 + (h / 100));
                            if (pop >= max) {
                                pop = pop - ((pop - max) * (1/10));
                            }
                        }
                        pop -= incoming;
                        temp += (turns - temp);
                    }
                    shuttle.remove(ind);
                    turns = 100;
                }
            }

            ret = pop;

        } else if (planet.getOwner() == Owner.OPPONENT) {
            turns = 100;
            pop = -planet.getPopulation();
            while(shuttle.size() > 0) {

                for (int i = 0; i < shuttle.size(); i++) {
                    if (shuttle.get(i).getTurnsToArrival() < turns) {
                        turns = shuttle.get(i).getTurnsToArrival();
                        ind = i;
                    }
                } // grabs index of minimum turns

                int h = planet.getHabitability();
                int max = (int) planet.getSize();
                long incoming = shuttle.get(ind).getNumberPeople();

                for (int i = 0; i < turns - temp; i++) { // temp is a counter of how many steps have already been taken
                    pop = pop * (1 + (h / 100));
                    if (pop >= max) {
                        pop = pop - ((pop - max) * (1/10));
                    }
                } // updated values of the population on the planet with growth/decay

                if (shuttle.get(ind).getOwner() == Owner.OPPONENT && shuttle.get(ind).getTurnsToArrival() <= numTurns) {
                    pop -= shuttle.get(ind).getNumberPeople();
                } else if (shuttle.get(ind).getTurnsToArrival() <= numTurns) { // Owner is we
                    pop += incoming;
                }
                temp += (turns - temp);
                shuttle.remove(ind);

            }
            ret = pop;
        }
        return ret;
    }


}