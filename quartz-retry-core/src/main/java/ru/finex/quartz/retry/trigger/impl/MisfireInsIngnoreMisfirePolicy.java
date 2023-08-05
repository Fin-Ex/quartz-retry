/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.finex.quartz.retry.trigger.impl;

/**
 *
 * @author HOME
 */
public class MisfireInsIngnoreMisfirePolicy extends MisfireInstruction{

    @Override
    public void getMisfireBehavior(RetryCronScheduleBuilder cb){
        cb.withMisfireHandlingInstructionIgnoreMisfires();        
    };
}
