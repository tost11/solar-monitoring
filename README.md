# solar-monitoring
solar monitoring web server

Open Points

    "/api/user/login"
        If credetiols true set User to ContextHolder

    "/api/user/register"
        If username is not taken and Pettern ist fulfil register User in Neo3j and set User to ContextHolder

    "/api/solar/data/selfmade"
        add data to your system by Token

    "/api/solar/data/selfmade/mult"
        add list of data to your system by Token

    "/api/solar/data/selfmade/consumption/device"
        add data to your system by Token

    "/api/solar/data/selfmade/consumption/device/mult"
        add list of data to your system by Token

    "/api/solar/data/selfmade/inverter"
        add data to your system by Token

    "/api/solar/data/selfmade/inverter/mult"
        add list of data to your system by Token

    "/api/solar/data/selfmade/consumption"
        add data to your system by Token

    "/api/solar/data/selfmade/consumption/mult"
        add list of data to your system by Token

Points Where you need Access

    "/api/user/edit"
        Edit User if You hat Admin Permisions 

    "/api/user/admin/findUser/{name}"
        Rurn List of User serche by Name if you hat Admin Permisions

    "/api/user/findUser/{name}""
        Return User serche by Name

    "/api/system"
        Create a New System

    "/api/system/edit"
        edit System if you have Permissions

    "/api/system/{systemID}"
        return the system when you are Manager, Admin ,Owner

    "/api/system/all"
        return List of system's wher you have Permisions

    "/api/system/delete/{id}"
        delete System if you are Owner
        
    "/api/system/addManageBy"
        add Manager if you are Admin or Owner

    "/api/system/allManager/{systemId}"
        return all Manager on a system if you are Admin or Owner

    "/api/system/deleteManager/{managerId}/{systemId}"
        delete Manager relation if you are Admin or Owner

    "/api/system/newToken/{id}"
        create a new SystemToken if you are Admin or Owner

    "/api/migration"
        Migriert den typen
