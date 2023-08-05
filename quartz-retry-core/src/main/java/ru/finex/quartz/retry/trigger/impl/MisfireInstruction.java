/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.finex.quartz.retry.trigger.impl;

import static ru.finex.quartz.retry.trigger.RetryCronTrigger.*;

/**
 *
 * @author HOME
 */
public abstract class MisfireInstruction {
    
    public static MisfireInstruction createMisfireHandler(int misfireInstruction) {
        switch (misfireInstruction) {
            case MISFIRE_INSTRUCTION_DO_NOTHING:
                return new MisfireInstDoNothing();
            case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
                return new MisfireInsFireOnceNow();
            case MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY:
                return new MisfireInsIngnoreMisfirePolicy();
            default:
                return new MisfireInsDefault(misfireInstruction);
        }
    }
    public abstract void getMisfireBehavior(RetryCronScheduleBuilder cb);

}
