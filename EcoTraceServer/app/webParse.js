const parsing = {
    "https://www.nationalgeographic.com/environment" : {
        news: ".GridPromoTile",

        newsImage: {
            gotoUrl: true,
            classIn: 'img[data-testid="prism-image"]',
            widget: null,
            widgetAttr: "src",
            widgetValue: false
        },
        newsTitle: {
            classIn: ".GridPromoTile__Tile",
            widget: "a",
            widgetAttr: "aria-label",
            widgetValue: false
        },
        newsLink: {
            classIn: ".GridPromoTile__Tile",
            widget: "a",
            widgetAttr: "href",
            widgetValue: false
        },

        source: "National Geographic",
        isRu: false
    }, 
    "https://www.ecosociety.ru/" : {
        news: ".elementor-posts-container",

        newsTitle: {
            classIn: ".elementor-post__title",
            widget: "a",
            widgetAttr: null,
            widgetValue: true,
        },
        newsImage: {
            classIn: ".elementor-post__thumbnail",
            widget: "img",
            widgetAttr: "src",
            widgetValue: false,
            gotoUrl: false
        },
        newsLink: {
            classIn: ".elementor-post__title",
            widget: "a",
            widgetAttr: "href",
            widgetValue: false,
        },

        source: "Российское экологическое общество",
        isRu: true
    }
};

module.exports = parsing