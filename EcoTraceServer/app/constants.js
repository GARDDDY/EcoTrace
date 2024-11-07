const express = require('express');
const router = express.Router();

const eventRoles = ["Создатель", "Помощник", "Участник"]
const groupRanks = ["Владелец", "Совладелец", "Следящий", "Участник"]

const userRanks = ["Крутой"]
            
const usersFilters = [
    { name: "Активный", description: "\"Активный\" пользователь всегда готов присоединиться к новым мероприятиям" },
    { name: "Веселый", description: "\"Веселый\" пользователь" },
    { name: "Умный", description: "Настоящий нарциссист" },
    { name: "Онлайн", description: "Пользователь часто бывает в сети" },
    { name: "Творческий", description: "\"Творческий\" пользователь, увлеченный искусством" },
    { name: "Экстраверт", description: "\"Экстраверт\" пользователь, любящий общение" },
];

const eventsFilters = [
    { name: "На улице", description: "Часть или все мероприятие проходит на улице" },
    { name: "В помещении", description: "Часть или все мероприятие проходит в помещении" },
    { name: "Онлайн", description: "Мероприятие проходит в виртуальном формате" },
    { name: "Семейное", description: "Мероприятие, подходящее для всей семьи" },
];

const groupsFilters = [
    { name: "Мероприятия", description: "Группа, в которой пользователи делятся интересными мероприятиями" },
    { name: "По интересам", description: "Группа, в которой пользователи просто общаются" },
    { name: "Спорт", description: "Группа для обсуждения спортивных мероприятий" },
    { name: "Творчество", description: "Группа для любителей искусства и творчества" },
];

const filterColors = [
    { name: "#00FA9A", description: "#1A3329" },
    { name: "#FF7F50", description: "#33201A" },
    { name: "#00CED1", description: "#1A3333" },
    { name: "#DA70D6", description: "#331A32" },
    { name: "#FFD700", description: "#4B4B0E" },
    { name: "#FF4500", description: "#4B1A1A" },
];

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