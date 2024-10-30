types = list(map(int, input().split()))
counts = list(map(int, input().split()))

import random, time
counts += [random.randint(1, 10) for _ in range(len(types) - len(counts))]

def generateUsers(num: int):
    pass


def generateEvents(num: int):
    pass


def generateGroups(num: int):
    groups = []
    for group in range(num):
        ## group info

        req1 = f"""insert into groups.group (groupId, groupName, groupCreatorId, filters, groupType, groupAbout, groupRules, groupRulesImage) 
                values('group_{group}', 'Группа №{group}', null, 
                '{",".join([str(random.randint(1, 4)) for _ in range(random.randint(0, 5))])}',
                {random.randint(0, 1)}, 'Описание группы №{group}'),
                'Правила группы №{group} таковы:\n1. Ну это, ну там... аа.. ээ.. так и живем', null"""
        
        groups.append([req1])
        ## group posts

        for post in range(group+2):
            req2 = f"""insert into groups.posts (groupId, postId, postTime, postCreatorId, postContentText, postContentImage)
                    values('group_{group}', {post}, 
                    {int(time.time())}, null, 
                    'Это запись под номером {post} в группу под названием \"Группа №{group}\"!', null)"""
            
            groups[group].append(req2)

        ## group comments

    return groups



for type, count in zip(types, counts):
    match type:
        case 1: print(generateUsers(count))
        case 2: print(generateEvents(count))
        case 3: print(generateGroups(count))