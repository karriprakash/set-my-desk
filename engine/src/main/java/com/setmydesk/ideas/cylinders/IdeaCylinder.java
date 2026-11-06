package com.setmydesk.ideas.cylinders;

import com.setmydesk.core.Cylinder;
import com.setmydesk.ideas.IdeaContext;

/** Marker type so Spring can collect the idea cylinders in {@code @Order}. */
public interface IdeaCylinder extends Cylinder<IdeaContext> {
}
