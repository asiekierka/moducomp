/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2013. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computer.api;

public interface ILuaObject
{
    public String[] getMethodNames();
    public Object[] callMethod( ILuaContext context, int method, Object[] arguments ) throws Exception;
}
