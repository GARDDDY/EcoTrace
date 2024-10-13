const express = require('express');
const router = express.Router();

const eventRoles = ["Создатель", "Помощник", "Участник"]
const groupRanks = ["Владелец", "Совладелец", "Следящий", "Участник"]

const userRanks = ["Крутой"]
            
const usersFilters = {
    "Активный": "\"Активный\" пользователь всегдат\nготов присоединиться к новым мероприятиям",
    "Веселый": "\"Веселый\" пользователь",
    "Умный": "Настоящий нарциссист",
    "Онлайн": "Пользователь часто бывает в сети"
}

const eventsFilters = {
    "На улице": "Часть или все мероприятие проходит на улице",
    "В помещении": "Часть или все мероприятие проходит в помещении"
}

const groupsFilters = {
    "Мероприятия": "Группа, в которой пользователи делятся интересными мероприятиями",
    "По интересам": "Группа, в которой пользователи просто общаются"
}

const filterColors = {
    "#00FA9A": "#1A3329",
    "#FF7F50": "#33201A",
    "#00CED1": "#1A3333",
    "#DA70D6": "#331A32"
}

const merge = [
    eventRoles,
    groupRanks,
    userRanks,
    usersFilters,
    eventsFilters,
    groupsFilters,
    filterColors
]


router.get('/constants', async (req, res) => {
    const type = req.query.type || -1;
    
    res.send(merge[type])
});

module.exports = router;