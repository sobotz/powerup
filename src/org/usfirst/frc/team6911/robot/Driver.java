package org.usfirst.frc.team6911.robot;

import java.util.HashMap;

import org.usfirst.frc.team6911.robot.Robotmap.DriveMode;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDOutput;
import edu.wpi.first.wpilibj.PIDSource;
import edu.wpi.first.wpilibj.PIDSourceType;

public final class Driver implements PIDOutput {

	/////// CLASS DESCRIPTION ///////////////
	/*
	 * This class handle the robot driving Specifically the function "Drive"
	 */

	///////////// DRIVETRAIN INSTANCIATION/////////////////
	private static DriveMode driveMode;
	private static OI driverJoystick;
	private static DifferentialDrive Driver;
	private static SpeedControllerGroup m_left;
	private static SpeedControllerGroup m_right;

	////////////////////////////////////////////////////////
	
	//////////////////// PID INSTANCIATION /////////////////

	////////////////// GYRO PID Coefficients////////////////
	private static PIDController GyroPid;
	private static double kP = 0.04;
	private static double kI = 0.0;
	private static double kD = 0.0;

	////////////////// ENCODERS PID Coefficients////////////
	private static PIDController EncoderPid;
	private static double ekP = 0.0;
	private static double ekI = 0.0;
	private static double ekD = 0.0;

	//////// PID output//////////////////
	private static double kPdeviation;//// GyroPID
	private static double kPspeed;/////// EncoderPID

	///////////// Scheduler////////////////////////////////////
	///////////// Handle the autonomous Paths//////////////////
	private static boolean goToNextStep;
	private static int stepPosition;
	static char gameData;

	private static HashMap<Integer, Boolean> Steps = new HashMap<Integer, Boolean>();

	// *******************************************************//

	/*
	 * To create an instance of this class, it takes An Enumeration Object in
	 * parameter, Specifically a Drivemode Enumeration Object in @Robotmap class
	 * that allow to specify the way the robot going to be driving
	 * (ARCADEDRIVE,TANKDRIVE,CURVATUREDRIVE. see the online doc for more
	 * explanations about the "drive mode".
	 * https://wpilib.screenstepslive.com/s/currentCS/m/java/l/599700-getting-your-
	 * robot-to-drive-with-the-robotdrive-class
	 */
	public Driver(DriveMode mdrivemode) {

		driveMode = mdrivemode;
		m_left = new SpeedControllerGroup(Robotmap.frontLeftMotor, Robotmap.rearLeftMotor);
		m_right = new SpeedControllerGroup(Robotmap.frontRightMotor, Robotmap.rearRightMotor);

		driverJoystick = new OI(Robotmap.driverJoystick);

		m_left.setInverted(true);
		m_right.setInverted(true);

		Driver = new DifferentialDrive(m_left, m_right);

		/////////Initialization/////////////////
		GyroPID();
		
		EncoderPID();
        
		driveTrainEncoders();

		gameData = DriverStation.getInstance().getGameSpecificMessage().charAt(0);

		stepPosition = 1;
		
	}

	//// This function allows to drive in teleop MODE//////////////
	public void Drive() {
		if (driverJoystick.getLeft_Y_AXIS() <= 0.1 && driverJoystick.getLeft_Y_AXIS() >= -0.1) {
			// SmartDashboard.putBoolean("Moving", false);
			// Robotmap.ahrs.zeroYaw();
		} else {
			// SmartDashboard.putBoolean("Moving", true);
		}

		if (driveMode == DriveMode.ARCADE & driverJoystick.getRtrigger() == 0) {
			Driver.arcadeDrive(driverJoystick.getLeft_Y_AXIS(), -driverJoystick.getRight_X_AXIS());
			Driver.setMaxOutput(0.8);
		} 
		
		else if (driveMode == DriveMode.ARCADE & driverJoystick.getRtrigger() != 0) {
			GyroPid.setSetpoint(0.0f);
			GyroPid.enable();
			Driver.arcadeDrive(driverJoystick.getLeft_Y_AXIS(), -kPspeed, false);
			Driver.setMaxOutput(0.7);
		}

	}

	/////////// Main function for autonomous Mode/////////////
	public void autonomousDrive(double speed) {
		if (!GyroPid.isEnabled()) {
			GyroPid.enable();
		}

		Driver.arcadeDrive(speed, -kPdeviation);

		SmartDashboard.putNumber("Encoder Average",
				(Robotmap.rEncoder.getDistance() + Robotmap.lEncoder.getDistance()) / 2);
//
	}

	////////// PIDs Instantiation///////////
	private void GyroPID() {

		GyroPid = new PIDController(kP, kI, kD, Robotmap.ahrs, this);
		GyroPid.setInputRange(-180.0, 180.0);
		GyroPid.setOutputRange(-1, 1);
		GyroPid.setPercentTolerance(95.0f);
		GyroPid.setContinuous(true);
	}

	//// Getting the PID output for The GyroPID
	@Override
	public void pidWrite(double output) {
		// TODO Auto-generated method stub
		kPdeviation = output;
		SmartDashboard.putNumber("Gyro PID Output", output);

	}
	//////////////////////////////////////////////////////////////////////////////

	////////////////////// ENCODER PID STUFF//////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////
	private void EncoderPID() {
		EncoderPid = new PIDController(ekP, ekI, ekD, new EncodersAverage(), new EncoderPIDOutput());
		GyroPid.setOutputRange(-1, 1);
		GyroPid.setPercentTolerance(99.0f);
		GyroPid.setContinuous(true);
	}
    
	
	/////////// Inside Class ///////////////////////////////////////////////////
	private class EncoderPIDOutput implements PIDOutput {

		@Override
		public void pidWrite(double output) {
			// TODO Auto-generated method stub

			kPspeed = output;

			SmartDashboard.putNumber("Encoder PID output", output);
		}

		// return (Robotmap.rEncoder.getDistance() + Robotmap.lEncoder.getDistance())/2;

	}

	private class EncodersAverage implements PIDSource {

		@Override
		public double pidGet() {
			// TODO Auto-generated method stub

			return (6*3.14) *((Robotmap.rEncoder.get()*(-1) + Robotmap.lEncoder.get())/2)/360;
		}

		@Override
		public void setPIDSourceType(PIDSourceType pidSource) {
			// TODO Auto-generated method stub
		}

		@Override
		public PIDSourceType getPIDSourceType() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	/////////////////////////////////////////////////////////////////////////////

	//////////// This function handle the range finder///////////////////////////
	public void rangeFinder() {
		Robotmap.ultraSonic.setAutomaticMode(true);
		Robotmap.ultraSonic.setEnabled(true);
		SmartDashboard.putNumber("RangeFinder", Robotmap.ultraSonic.getRangeInches());
	}
	
	/////////// Set up the encoders ////////////////////////////////////////////
	public void driveTrainEncoders() {

	//////////////// Encoders///////////////////////////////////////////////////
	///// Pulse per Revolution////
	double PPR = 1440;
	///// Cycle per Revolution/////
	double CPR = 360;

	////// Distance per Pulse/////
	double DPP = PPR / CPR;

	Robotmap.lEncoder.setMaxPeriod(.1);
	Robotmap.lEncoder.setMinRate(10);
	Robotmap.lEncoder.setDistancePerPulse(DPP);
	Robotmap.lEncoder.setReverseDirection(true);
	Robotmap.lEncoder.setSamplesToAverage(7);


	Robotmap.rEncoder.setMaxPeriod(.1);
	Robotmap.rEncoder.setMinRate(10);
	Robotmap.rEncoder.setDistancePerPulse(DPP);
	Robotmap.rEncoder.setReverseDirection(true);
	Robotmap.rEncoder.setSamplesToAverage(7);
	
}

	/////////////// Autonomous Command////////////////////////////////////////////
	
	/*
	 * During autonomous for any paths we choose we will have to repeat some typical action like Rotate To a certain angle
	 * drive to a certain distance .......
	 * That the purpose of the following method (RotateTo();, DriveTo();)
	 */
	
	
	// Calling this method will make the robot to turn the angle specified in parameter
	// In order to turn the we use a PID loop, we don't apply a constant speed to turn the robot, the PID controller 
	// would apply a constant speed to turn the robot until the gyro states that the angle has be changed to the angle wanted
	
	public static Boolean RotateTo(double angle) {
		GyroPid.setSetpoint(angle);
		
		GyroPid.enable();
		Driver.arcadeDrive(0.0, -kPdeviation);
		
		if (GyroPid.onTarget()) {
			stabilizer();
			return goToNextStep = true;
		} else {
			return goToNextStep = false;

		}

	}
    
	// Calling this function will make the robot to drive a certain distance
	// we don't use a constant speed, cause if we did we would overshooot (we could miss the set point) the target.
	// then we use A PID loop that will apply a proportional speed until the set point is reached in this case
	// until the robot travels the distance wanted 
	private static Boolean DriveTo(double distance) {
		EncoderPid.setSetpoint(distance);
		GyroPid.setSetpoint(0.0f);
		
			EncoderPid.enable();
			GyroPid.enable();
			Driver.arcadeDrive(-kPspeed, -kPdeviation);
			
		
		if (EncoderPid.onTarget()) {
			stabilizer();
			return goToNextStep = true;
		}else {
			return goToNextStep = false;
		}
	}

	////////////////// Handle autonomous Paths////////////////////////////////////

	public void StepsManager(String A, int S) {
		int mStation = S;

		if (mStation == 1) {
			if (gameData == 'L') {
				Steps.put(1, true);
				Steps.put(2, false);
				Steps.put(3, false);
			} else if (gameData == 'R') {
				
				Steps.put(1, true);
				Steps.put(2, false);
				Steps.put(3, false);
				Steps.put(4, false);
			}

		}
		
		if (mStation == 2) {
			if (gameData == 'L') {
				Steps.put(1, true);
				Steps.put(2, false);
				Steps.put(3, false);
			} else if (gameData == 'R') {
				
				Steps.put(1, true);
				Steps.put(2, false);
				Steps.put(3, false);
				Steps.put(4, false);
			}

		}
		
		if (mStation == 3) {
			if (gameData == 'L') {
				Steps.put(1, true);
				Steps.put(2, false);
				Steps.put(3, false);
			} else if (gameData == 'R') {
				
				Steps.put(1, true);
				Steps.put(2, false);
				Steps.put(3, false);
				Steps.put(4, false);
			}

		}
	}

	public static void StepPositionManager() {
		if (goToNextStep) {
			Steps.put(stepPosition, false);

			stepPosition = stepPosition + 1;

			Steps.put(stepPosition, true);

		}
	}

	public static void Scheduler(String A, int S) {
		// String mSide = A;
		int mStation = S;

		if (mStation == 1) {

			if (gameData == 'L') {

				if (Steps.get(1)) {

					DriveTo(120);
				}

				if (Steps.get(2)) {
					RotateTo(45.0f);

				}

				if (Steps.get(3)) {
					DriveTo(60);
				}

			}

			else if (gameData == 'R') {
             
			}
		}

		if (mStation == 2) {

		}

		if (mStation == 3) {

		}

		StepPositionManager();
	}

	///////// This function act like a stabilizer it reset everything, and allow the
	///////// PID to be balanced properly///////////////
	public static void stabilizer() {

		GyroPid.disable();
		GyroPid.reset();
		Robotmap.ahrs.zeroYaw();
		PIDsetCoefficient();
		GyroPid.setSetpoint(0.0f);

		EncoderPid.disable();
		EncoderPid.reset();
		EncoderPid.setSetpoint(0.0f);
		Robotmap.lEncoder.reset();
		Robotmap.rEncoder.reset();

		Timer.delay(0.07);
	}

	///////// This function is used to reset the Gyro angle/////////////
	public void resetYaw() {
		if (driverJoystick.getA()) {
			GyroPid.disable();
			GyroPid.reset();
			Robotmap.ahrs.zeroYaw();
			Timer.delay(0.05);
		}
	}

	////// This function is used to reset the encoders///////////
	public void resetencoder() {
		if (driverJoystick.getB()) {
			Robotmap.lEncoder.reset();
			Robotmap.rEncoder.reset();
		}

	}

	///////// This function is used to put value to the ShuffleBoard/////////////
	public void Dashboard() {
		// SmartDashboard.putNumber("Left Y AXIS", driverJoystick.getLeft_Y_AXIS());
		// SmartDashboard.putNumber("Right X AXIS", driverJoystick.getRight_X_AXIS());
		SmartDashboard.putNumber("YAW angle", Robotmap.ahrs.getYaw());
		SmartDashboard.putBoolean("isConneted   ", Robotmap.ahrs.isConnected());
		SmartDashboard.putBoolean("isMoving   ", Robotmap.ahrs.isMoving());
		SmartDashboard.putBoolean("isRotating   ", Robotmap.ahrs.isRotating());

		SmartDashboard.putNumber("gkP", kP);
		SmartDashboard.putNumber("gkI", kI);
		SmartDashboard.putNumber("gkD", kD);

		SmartDashboard.putNumber("ekP", ekP);
		SmartDashboard.putNumber("ekI", ekI);
		SmartDashboard.putNumber("ekD", ekD);
		
		SmartDashboard.putNumber("Left encoder", Robotmap.lEncoder.get());
		SmartDashboard.putNumber("Right encoder", Robotmap.rEncoder.get()*(-1));
	}

	/////////////// Those functions are used in test mode to Tune the PID
	/////////////// coefficient/////////////////
	public static void PIDsetCoefficient() {
		kP = Table.getNumber("kP", 0);
		kI = Table.getNumber("kI", 0);
		kD = Table.getNumber("kD", 0);
		GyroPid.setPID(kP, kI, kD);
		
		ekP = Table.getNumber("ekP", 0);
		ekI = Table.getNumber("ekI", 0);
		ekD = Table.getNumber("ekD", 0);
		EncoderPid.setPID(ekP, ekI, ekD);
	}



	
	
	
	
	///////// Disable the  motors ////////////////////////////////////////
	public void disablemotor() {

		Driver.stopMotor();
	}
}
