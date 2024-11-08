const express = require('express');
const router = express.Router();

const eventRoles = ["Создатель", "Помощник", "Участник"]
const groupRanks = ["Владелец", "Совладелец", "Следящий", "Участник"]

const userRanks = ["Крутой"]
            
const usersFilters = [
    { name: "Активный", description: "\"Активный\" пользователь всегда готов к новым начинаниям и активен в любом деле." },
    { name: "Веселый", description: "Пользователь, который всегда в хорошем настроении и приносит позитив в любое общение." },
    { name: "Открытый", description: "Пользователь, который легко находит общий язык с окружающими и всегда открыт для новых знакомств и идей." },
    { name: "Онлайн", description: "Пользователь, который часто бывает в сети и не пропускает интересные события и обсуждения.и" },
    { name: "Трудолюбивый", description: "Пользователь, который не боится вкладываться в работу и всегда доводит начатое до конца." },
    { name: "Экстраверт", description: "Пользователь, любящий общение" },
];

const eventsFilters = [
    { name: "На улице", description: "Часть или все мероприятие проходит на улице" },
    { name: "В помещении", description: "Часть или все мероприятие проходит в помещении" },
    { name: "Образовательное", description: "" },
    { name: "Для всей семьи", description: "" },
];

const groupsFilters = [
    { name: "Мероприятия", description: "Группа, в которой пользователи делятся интересными мероприятиями" },
    { name: "По интересам", description: "Группа, в которой пользователи просто общаются" },
    { name: "Для новичков", description: "" },
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