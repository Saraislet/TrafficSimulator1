import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JPanel;

public class Lane extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int windowWidth = 1000;
	
	ArrayList<Car> cars = new ArrayList<Car>();

	private int numCars;
	private int laneIndex = 0;
	private Random rand = new Random();
	private int flaggedCarIndex;
	private int newLaneIndex;
	public boolean flagLaneChange = false;
	
	public Lane(int k) {
		numCars = k;
		for (int i = 0; i < numCars; i++) {
			// generate random color, a random lane, generate a car, then set the color and lane
			float r = 0; 	// only aggressive cars will include a red hue
			float g = rand.nextFloat();
			float b = rand.nextFloat();
			Color randomColor = new Color(r, g, b);
			double xPosition = windowWidth * i /numCars;
			double velocity = 2 + 0.5 * Math.abs(rand.nextGaussian());
			Car newCar = new Car(this, xPosition, velocity);
			newCar.setColor(randomColor);
			newCar.setLaneIndex(laneIndex);
			
			// roll to determine if car will be aggressive, then generate and set aggressionLevel
			float aggressionRoll = rand.nextFloat();
			if (aggressionRoll > TrafficSimulator.aggressionChance) {
				double aggressionLevel = 3 * rand.nextDouble();
				newCar.setAggressionLevel(aggressionLevel);
			}
			cars.add(newCar);
		}
	}
	
	// method to generate a car in the lane
	public void addCar(Car newCar, int direction) {
		numCars = cars.size();
		Car car = newCar;
		double x = car.getXPosition();
		car.setLaneIndex(laneIndex);
		car.setLane(this);
		car.setFlagLaneChanged(direction);
//		System.out.println(" Lane " + laneIndex + " has " + (numCars + 1 )+ " cars after a car changes to this lane.");
		
		int maxIndex = 0;
		int minIndex = 0;
		
		if (numCars != 0) {
			if (cars.get(0).getXPosition() > cars.get(numCars - 1).getXPosition()) {
				maxIndex = findMaxIndex(0, numCars - 1);
				minIndex = maxIndex + 1;
			} else {
				maxIndex = numCars - 1;
				minIndex = 0;
			}
		}
		
		if (numCars == 0) {
			cars.add(car);
		} else if (x > cars.get(maxIndex).getXPosition()) {
			cars.add(maxIndex + 1, car);
		} else if (x < cars.get(minIndex).getXPosition()) {
			cars.add(minIndex, car);
		} else if (x > cars.get(0).getXPosition()) {
			int newIndex = insert(x, 0, maxIndex);
			cars.add(newIndex, car);
		} else if (x > cars.get(numCars - 1).getXPosition()) {
			cars.add(numCars, car);
		} else {
			int newIndex = insert(x, minIndex, numCars - 1);
			cars.add(newIndex, car);
		}
		
//		if (numCars == 0) {
//			cars.add(car);
//		} else if (x < cars.get(0).getXPosition()) {
//			cars.add(0, car);
//		} else if (x > cars.get(numCars - 1).getXPosition()) {
//			cars.add(car);
//		} else {
//			int newIndex = insert(x, 0, numCars - 1);
//			cars.add(newIndex, car);
//		}
		numCars++;
	}
	
	public void paintComponent(Graphics graphics) {
		for (Car myCar : cars) {
			myCar.paintComponent(graphics);
		}
	}
	
	public void update() {
		numCars = cars.size();
		if (numCars == 1) {
			cars.get(0).update(windowWidth);
		} else if (numCars > 1) {
			for (int i = 0; i < numCars; i++) {
				Car carA = cars.get(i);
				Car carB = cars.get((i + 1) % numCars);
				double distance = (carB.getXPosition() - carA.getXPosition());
				distance = (distance >= 0.0 ? distance : distance + windowWidth);
				carA.updateAcceleration(distance, carB.getVelocity());
				carA.update(windowWidth);
			}
		}
		// if a car in this lane has flagged that they will change lanes, run methods to change lanes for that car
		if (flagLaneChange == true) {
			this.changeLane(flaggedCarIndex);
			flagLaneChange = false;
		}
	}
	
	// method to check for cars between two x coordinates
	public boolean checkLane(double xStart, double xEnd) {
		for (int i = 0; i < numCars; i++) {
			if (cars.get(i).getXPosition() >= xStart && cars.get(i).getXPosition() <= xEnd) {
				return false;
			}
		}
		return true;
	}
	
	// method to flag a lane to indicate that a car wants to change lanes
	public void flagLaneChange(Car car, int nextLaneIndex) {
		flaggedCarIndex = cars.indexOf(car);
		newLaneIndex = nextLaneIndex;
		flagLaneChange = true;
	}
	
	// method to help a car change lanes
	public void changeLane(int carIndex) {
		Car car = cars.get(carIndex);
		cars.remove(carIndex);
		numCars = cars.size();
//		System.out.print("Lane " + laneIndex + " has " + numCars + " cars after one left.");
		TrafficSimulator.lanes.get(newLaneIndex).addCar(car, laneIndex - newLaneIndex);
	}
	
	// returns the index for inserting a car
	public int insert(double x, int start, int end) {
		int size = end - start;
		if (size == 0) {
			if (x < cars.get(start).getXPosition()) {
				return start;
			} else {
				return start + 1;
			}
		} else if (size == 1) {
			return start + 1;
		} else {
			int pivot = (int) ((end + start) / 2 );
			if (x < cars.get(pivot).getXPosition()) {
				return insert(x, start, pivot);
			} else {
				return insert(x, pivot, end);
			}
		}
	}
	
	// returns the index for the car with the maximum x position in the list
	public int findMaxIndex(int start, int end) {
		int pivot = (int) ((end + start) / 2);
		if (end - start == 0) {
			return start;
		} else if (end - start == 1) {
			return start;
		} else {
			if (cars.get(0).getXPosition() > cars.get(pivot).getXPosition()) {
				return findMaxIndex(start, pivot);
			} else {
				return findMaxIndex(pivot, end);
			}
		}
	}
	
	// method to find the starting x coordinate of the largest gap between cars
	public double findGap() {
		double xMin = 0.0;
		if (numCars == 0) {
			return xMin;
		} else if (numCars == 1) {
			return cars.get(0).getXPosition();
		} else {
			
			// find largest gap from sorted array of length at least 2, and set xMin to the start of the gap
			double maxGap = 0.0;
			for (int i = 0; i < numCars; i++) {
				if ( maxGap <= cars.get((i + 1) % numCars).getXPosition() - cars.get(i).getXPosition()) {
					maxGap = cars.get((i + 1) % numCars).getXPosition() - cars.get(i).getXPosition();
					xMin = cars.get(i).getXPosition();
				}
			}
			
			return xMin;
		}
	}
	
	// obsolete quicksort methods
//	public double[] quicksort(double[] A,int p, int r) {
//		// To do: write heapsort or quicksort algorithm
//		int q = 0;
//		if (p < r) {
//			q = partition(A, p, r);
//			A = quicksort(A, p, q-1);
//			A = quicksort(A, q+1, r);
//		}
//		
//		return A;		
//	}
//	
//	public int partition(double[] A, int p, int r) {
//		double x = A[r];
//		int i = p-1;
//		
//		for (int j = p; j < r; j++) {
//			if (A[j] <= x) {
//				i++;
//				double temp = A[i];
//				A[i] = A[j];
//				A[j] = temp;
//			}
//		}
//		double temp = A[i+1];
//		A[i+1] = A[r];
//		A[r] = temp;
//		
//		return i+1;				
//	}
	
	// methods to get the number of cars in this lane
	public int getLength() {
		return numCars;
	}
	
	//methods to get and set the index of this lane
	public int getIndex() {
		return laneIndex;
	}
	
	public void setIndex(int newIndex) {
		laneIndex = newIndex;
		
		for (Car myCar : cars) {
			myCar.setLaneIndex(laneIndex);
		}
	}
	
	// methods to get a Car from this lane
	public Car getCar(int i) {
		return cars.get(i);
	}
}
