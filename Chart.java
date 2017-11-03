package assignment10.fitbit;

import java.util.Arrays;

import org.omg.CORBA.SystemException;

import com.sun.prism.paint.Color;

import sedgewick.StdDraw;


public class Chart {

	public int width;
	public int height;
	private int startHeight; 
	private double xStart; 
	private double yStart; 
	private double endX; 
	private double endY; 
	private int numXTicks = 10; 
	private int numYTicks = 10; 
	private int multiplierX = 1; 
	private int multiplierY = 1; 
	private double dividerX = 1; 
	private double dividerY = 1; 
	private double shiftWidth = 0.0;
	private double shiftHeight = 0.0; 
	private String xLabel = ""; 
	private String yLabel = "";
	private String title = ""; 
	private int numPoints = 200000; 
	private double points[][] = new double[this.numPoints][3];
	private double lines[] = new double[500];
	private java.awt.Color penColor; 
	private int count = 0; 
	private int lineCount = 0; //for indexing line array
	private double diffX =0; 
	private double dynamicPoint;
	private int lastPoint = this.numPoints; //last element needed in array 

	public Chart (int width, int startHeight, int height){
		StdDraw.enableDoubleBuffering();
		StdDraw.setCanvasSize(700, 600);
		this.width = width; 
		this.startHeight = startHeight; 
		this.height = height + startHeight; 
		this.xStart = this.width/7.0; 	
		this.yStart = (this.height+this.startHeight)/9.0;
		this.endX = width+this.xStart; 
		this.endY = height+this.yStart; 
		StdDraw.setXscale(0, this.endX);
		StdDraw.setYscale(startHeight, this.endY);
		this.dynamicPoint = (1.3)*(this.xStart+this.width)/2;
		this.multiplierX = this.width/10; 
		//		if (width >= 100){    // Sets number of tick marks, finds in what intervals to label axis, and finds multliplier for printing the label
		//			this.numXTicks = 10;
		//			this.multiplierX = 10; 
		//			this.dividerX = width/50; 
		//		}
		//		else { 
		//			this.numXTicks = 10;
		//			this.multiplierX = 5; 
		//			this.dividerX = 1;
		//		}
		//		if (height >= 100){
		//			this.numYTicks = 10;
		//			this.multiplierY = 10; 
		//			this.dividerY = height/50; 
		//		}
		//		else { 
		//			this.numYTicks = 10 ; 
		//			this.multiplierY = 5;
		//			this.dividerY = 1; 
		//		}

		drawAxis();


	}

	public void drawLine(double x) { 
		lines[this.lineCount] = x; 
		this.lineCount +=1; 
		StdDraw.setPenColor(StdDraw.RED);
		StdDraw.setPenRadius(0.1); 
		StdDraw.line(x - this.shiftWidth, this.yStart, x - this.shiftWidth, this.endY);

	}

	public void drawAxis(){
		StdDraw.setPenColor(StdDraw.WHITE);
		StdDraw.setPenRadius(0.002);
		StdDraw.line(this.xStart, this.yStart, this.endX, this.yStart); //x-axis
		StdDraw.line(this.xStart, this.yStart+this.startHeight, this.xStart, this.endY); //y-axis
		for (int i = 0; i < this.numXTicks; i++){ //DRAWS AND LABELS TICKS ON X AXIS
			StdDraw.line(this.xStart + (i*this.width/(double)this.numXTicks), this.yStart-(this.height/150.0), this.xStart + (i*this.width/(double)numXTicks), this.yStart+(this.height/150.0));
			if (i%this.dividerX == 0.0) { 
				StdDraw.text(this.xStart + (i*this.width/(double)this.numXTicks), this.yStart-((this.height-this.startHeight)/30.0), ""+ (i*this.multiplierX+ Math.round(this.shiftWidth)));
			}
		}


		for (int i = 0; i < this.numYTicks; i++){ // DRAWS AND LABELS TICKS ON Y AXIS
			StdDraw.line(this.xStart-(this.width/150.0), this.startHeight + (i*((this.endY - this.startHeight)/(double)this.numYTicks)), this.xStart+(this.width/150.0), this.startHeight + (i*((this.endY - this.startHeight)/(double)this.numYTicks)));
			if (i%this.dividerY == 0.0){
				StdDraw.text(this.xStart-(this.width/25.0), this.startHeight + (i*((this.endY - this.startHeight)/(double)this.numYTicks)), ""+ Math.round((this.startHeight + (i*((this.endY - this.startHeight)/(double)this.numYTicks))) *10.0)/10.0);
			}
		}

	}

	public void axis(String xAxis, String yAxis){
		this.xLabel = xAxis; 
		this.yLabel = yAxis; 	
		StdDraw.setPenColor(StdDraw.WHITE);
		StdDraw.text((this.width+this.xStart)/2.0, this.startHeight+((this.height-this.startHeight)/30.0) , xAxis);
		StdDraw.text(this.width/30.0, (this.height+this.yStart)/2.0, yAxis, 90);
	}

	public void title(String title){
		this.title = title; 
		StdDraw.setPenColor(StdDraw.WHITE);
		StdDraw.text((this.width+this.xStart)/2.0, this.endY - ((1.0/10.0)*(this.height - this.startHeight)) , title);
	}

	public void addPoint(double x, double y, java.awt.Color color, boolean notLine){
		if (notLine == true) { 
		this.penColor = color; 
		StdDraw.setPenColor(color);
		StdDraw.setPenRadius(0.05);
		if (this.count >= this.lastPoint){
			this.count = this.lastPoint; 
		}

		if (color == StdDraw.RED){
			this.points[this.count][2] = 0; 
		}
		if (color == StdDraw.BLUE){
			this.points[this.count][2] = 1; 
		}
		if (color == StdDraw.GREEN){
			this.points[this.count][2] = 2; 
		}
		if (color == StdDraw.YELLOW){
			this.points[this.count][2] = 3; 
		}

		this.points[this.count][0] = (this.xStart+x); 
		this.points[this.count][1] = (this.yStart+y); 


		StdDraw.point(this.points[this.count][0]  - this.shiftWidth, this.points[this.count][1]);

		} else if (notLine == false){ 
			this.points[this.count][2] = 4; 
			this.points[this.count][0] = (this.xStart + x); 
			this.points[this.count][1] = (this.yStart + y);
			if (this.count < this.numPoints-1){
				this.count +=1;
			}
			StdDraw.setPenColor(StdDraw.GREEN);
			StdDraw.setPenRadius(0.1); 
			StdDraw.line(x - this.shiftWidth, this.yStart, x - this.shiftWidth, this.endY);
		}
		if (this.count < this.numPoints-1){
			this.count +=1;
		}


		if (x >this.dynamicPoint){ // If Past a certain point, labels begin to move 
			this.diffX = (x - this.dynamicPoint) - this.shiftWidth; //Calculates how much the graph has to shift (how far the point extends past the dynamic point)
			this.lastPoint = this.count; 
			for (int i = this.numPoints - 1; i > 0; i--){
				if (this.count < this.numPoints - 2){
					this.points[this.count+1][0] = this.points[this.count][0];
					this.points[this.count+1][1] = this.points[this.count][1]; 
				}
			}
		}
		shiftX(); 
		StdDraw.show(); 
		
		
		

	}

	public void drawAllPoints(){
		StdDraw.setPenRadius(0.01);
//		for (int i =0; i < 500;  i++){ 
//			if (lines[i] != 0.0){
//				StdDraw.setPenColor(StdDraw.GREEN);
//				StdDraw.setPenRadius(0.002); 
//				StdDraw.line(lines[i] - this.shiftWidth + this.xStart, this.startHeight, lines[i] - this.shiftWidth + this.xStart, this.endY);
//			}
//		}
		for (int i = 0; i < this.lastPoint-1; i++){
			StdDraw.setPenRadius(0.005); 
			if (this.points[i][0] != 0 && this.points[i][0]-this.shiftWidth > this.xStart){
				if (this.points[i][2] == 0){
					StdDraw.setPenColor(StdDraw.RED);
				}
				if (this.points[i][2] == 1){
					StdDraw.setPenColor(StdDraw.BLUE);
				}
				if (this.points[i][2] == 2){
					StdDraw.setPenColor(StdDraw.GREEN);
				}
				if (this.points[i][2] == 3){
					StdDraw.setPenColor(StdDraw.YELLOW);
				}
				StdDraw.point(this.points[i][0] - this.shiftWidth, this.points[i][1]);

				if (this.points[i][2] == 4){
					StdDraw.setPenColor(StdDraw.GREEN);
					StdDraw.setPenRadius(0.002); 
					StdDraw.line(this.points[i][0] - this.shiftWidth, this.startHeight, this.points[i][0] - this.shiftWidth, this.endY);				}
				}

		}
		StdDraw.setPenColor(StdDraw.WHITE);
		if (fitbitReceiver.prevMode == 0){
			StdDraw.text(this.width*(26.0/30.0), (this.height -this.startHeight)*(8.0/11.0), fitbitReceiver.sumCT + " C");
			StdDraw.text(this.width*(26.0/30.0), (this.height - this.startHeight)*(7.0/11.0), "Steps: " + fitbitReceiver.sumStep);
			StdDraw.text(this.width*(26.0/30.0), (this.height - this.startHeight)*(6.0/11.0), "Restful Sleep: " + fitbitReceiver.sumSleep);
			StdDraw.text(this.width*(26.0/30.0), (this.height - this.startHeight)*(5.0/11.0), "Steps per hour: " + fitbitReceiver.stepRate);

		}
		if (fitbitReceiver.prevMode == 1){ 
			StdDraw.text(this.width*(26.0/30.0), this.height*(10.0/11.0), fitbitReceiver.sumCT + "C");
			StdDraw.text(this.width*(26.0/30.0), this.height*(9.0/11.0), "Steps: " + fitbitReceiver.sumStep);
			StdDraw.text(this.width*(26.0/30.0), this.height*(8.0/11.0), "Restful Sleep: " + fitbitReceiver.sumSleep);
			StdDraw.text(this.width*(26.0/30.0), this.height*(7.0/11.0), "Steps per hour: " + fitbitReceiver.stepRate);

		}

	}

	public void addCoordinateLabel(double x, double y){

		StdDraw.setPenColor(StdDraw.BLACK);
		StdDraw.setPenRadius(0.002);
		StdDraw.text((this.xStart+x)-this.shiftWidth, (this.yStart + y + (this.height/50.0)) - this.shiftHeight, "("+ x + ", "+ y + ")");

	}

	public void addLabel(double x, double y, java.awt.Color color, String string){
		StdDraw.setPenColor(color);
		StdDraw.setPenRadius(0.002);
		StdDraw.text((this.xStart+x)-this.shiftWidth, (this.yStart + y + (this.height/50.0)) - this.shiftHeight, string);
	}

	public void shiftX(){
		for (int i = 0; i < this.numPoints; i ++){
			this.points[i][0] -= this.diffX; 
		}

		StdDraw.clear(StdDraw.BLACK); 
		this.shiftWidth += this.diffX; //
		//System.out.println(this.shiftWidth);
		axis(this.xLabel, this.yLabel);
		title(this.title);
		drawAxis();	
		drawAllPoints(); 
	}

	public void shiftY(){

		StdDraw.clear(StdDraw.BLACK); 
		this.shiftHeight += this.height; 
		axis(this.xLabel, this.yLabel);
		title(this.title);
		drawAxis();
	}

	public void clear(){
		StdDraw.clear(StdDraw.BLACK);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}