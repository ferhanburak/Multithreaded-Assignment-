package seng315_1;

import java.util.Random;
import java.util.Scanner;

public class RabbitGame {

    private static int NUM_BOXES;
    private static int NUM_RABBITS;
    private static int CARROT_PRODUCE_RATE;
    private static int CARROT_TIMEOUT;
    private static int RABBIT_SLEEP_TIME;
    private static int activeThreads;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the number of boxes: ");
        NUM_BOXES = scanner.nextInt();

        System.out.print("Enter the number of rabbits: ");
        NUM_RABBITS = scanner.nextInt();

        System.out.print("Enter the carrot produce rate (milliseconds): ");
        CARROT_PRODUCE_RATE = scanner.nextInt();

        System.out.print("Enter the carrot timeout (milliseconds): ");
        CARROT_TIMEOUT = scanner.nextInt();

        System.out.print("Enter the rabbit sleep time (milliseconds): ");
        RABBIT_SLEEP_TIME = scanner.nextInt();

        scanner.close();

        RabbitGame game = new RabbitGame();
        game.startGame();
    }

    private void startGame() {
        Box[] boxes = new Box[NUM_BOXES];
        for (int i = 0; i < NUM_BOXES; i++) {
            boxes[i] = new Box(i);
        }

        Person person = new Person(boxes, CARROT_PRODUCE_RATE, CARROT_TIMEOUT);
        Thread personThread = new Thread(person);
        personThread.start();

        Rabbit[] rabbits = new Rabbit[NUM_RABBITS];
        Thread[] rabbitThreads = new Thread[NUM_RABBITS];
        activeThreads = NUM_RABBITS + 1;

        for (int i = 0; i < NUM_RABBITS; i++) {
            rabbits[i] = new Rabbit("Rabbit" + (i + 1), boxes, RABBIT_SLEEP_TIME);
            rabbitThreads[i] = new Thread(rabbits[i]);
            rabbitThreads[i].start();
        }

        try {
            personThread.join();
            for (Thread rabbitThread : rabbitThreads) {
                rabbitThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game over!");
        for (Rabbit rabbit : rabbits) {
            System.out.println(rabbit.getName() + " has " + rabbit.getPoints() + " points");
        }
    }

    private static class Rabbit implements Runnable {
        private final String name;
        private final Box[] boxes;
        private final int sleepTime;
        private int currentBox;
        private int points;
        private boolean reachedEnd;

        public Rabbit(String name, Box[] boxes, int sleepTime) {
            this.name = name;
            this.boxes = boxes;
            this.sleepTime = sleepTime;
            this.currentBox = 0;
            this.points = 0;
            this.reachedEnd = false;
        }

        public String getName() {
            return name;
        }

        public int getPoints() {
            return points;
        }

        @Override
        public void run() {
            while (currentBox < NUM_BOXES - 1) {
                try {
                    Thread.sleep(sleepTime);
                    jump();

                    if (currentBox == 99) {
                        reachedEnd = true;
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            synchronized (RabbitGame.class) {
                activeThreads--;

                if (reachedEnd || activeThreads == 0) {
                    RabbitGame.class.notify();
                }
            }
        }

        private void jump() {
            int nextBox = currentBox + 1;
            System.out.println(name + " jumps to box " + nextBox);
            currentBox = nextBox;

            if (boxes[currentBox].hasCarrot()) {
                eatCarrot();
            }
        }

        private void eatCarrot() {
            points++;
            System.out.println(name + " eats carrot in box " + currentBox);
            boxes[currentBox].removeCarrot();
            System.out.println("Carrot in box " + currentBox + " removed");
        }
       }

    private static class Person implements Runnable {
        private final Box[] boxes;
        private final int produceRate;
        private final int timeout;
        private final Random random;
        private final Rabbit[] rabbits;

        public Person(Box[] boxes, int produceRate, int timeout) {
            this.boxes = boxes;
            this.produceRate = produceRate;
            this.timeout = timeout;
            this.random = new Random();
            this.rabbits = new Rabbit[NUM_RABBITS];
        }

        @Override
        public void run() {
            for (int i = 0; i < NUM_RABBITS; i++) {
                rabbits[i] = new Rabbit("Rabbit" + (i + 1), boxes, RABBIT_SLEEP_TIME);
                new Thread(rabbits[i]).start();
            }

            while (true) {
                try {
                    Thread.sleep(produceRate);
                    produceCarrot();

                    
                    boolean allRabbitsReachedEnd = true;
                    for (Rabbit rabbit : rabbits) {
                        if (!rabbit.reachedEnd) {
                            allRabbitsReachedEnd = false;
                            break;
                        }
                    }

                    if (allRabbitsReachedEnd) {
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void produceCarrot() {
            int boxIndex = random.nextInt(NUM_BOXES);
            boxes[boxIndex].putCarrot();

            System.out.println("Person puts carrot to box " + boxIndex);

            try {
                Thread.sleep(timeout);
                boxes[boxIndex].removeCarrot();
                System.out.println("Carrot in box " + boxIndex + " removed");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Box {
        private final int index;
        private boolean hasCarrot;

        public Box(int index) {
            this.index = index;
            this.hasCarrot = false;
        }

        public int getIndex() {
            return index;
        }

        public synchronized void putCarrot() {
            hasCarrot = true;
        }

        public synchronized void removeCarrot() {
            hasCarrot = false;
        }

        public synchronized boolean hasCarrot() {
            return hasCarrot;
        }
    }
}
