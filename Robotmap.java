package org.usfirst.frc.team6911.robot;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.Victor;

public class Robotmap {
	
	
	static enum MotorType{
		
		VICTOR
	}
	
	

	//SpeedController Declaration
	
	static SpeedController frontLeftMotor = new Victor(4);
	static SpeedController rearLeftMotor = new Victor(3);
	static SpeedController frontRightMotor = new Victor(1);
	static SpeedController rearRightMotor = new Victor(2);
	
	
	//static SpeedController MotorType = new Victor();
	
	//Joy stick Declaration
	
	
	static Joystick driverJoystick = new Joystick(0);

}
