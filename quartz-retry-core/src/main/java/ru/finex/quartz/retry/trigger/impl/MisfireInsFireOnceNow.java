/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.finex.quartz.retry.trigger.impl;

import static ru.finex.quartz.retry.trigger.RetryCronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;

/**
 *
 * @author HOME
 */
public class MisfireInsFireOnceNow extends MisfireInstruction{

    @Override
    public void getMisfireBehavior(RetryCronScheduleBuilder cb){
        cb.withMisfireHandlingInstructionFireAndProceed();          
    };
    
}
