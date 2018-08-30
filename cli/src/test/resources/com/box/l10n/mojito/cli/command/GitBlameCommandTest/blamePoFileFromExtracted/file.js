// next line has a string
description = "100 character description:";

file_loc_15 = "15 min";

file_loc_day = "1 day";

file_loc_hour = "1 hour";

file_loc_year = "1 month";

// Test no text
number = 1 + 1;

// There should not be anything blamed here
function choose_line(num, str1, str2) {
    return num === 1 ? str1 : str2;
};

choose_line(number, "There is {number} car", "There are {number} cars");