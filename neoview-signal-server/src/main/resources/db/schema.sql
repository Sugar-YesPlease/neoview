-- 用户表：用于登录鉴权
CREATE TABLE IF NOT EXISTS tbl_user (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 会议表：一个会议就是一个房间
CREATE TABLE IF NOT EXISTS tbl_meeting (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    room_id VARCHAR(100) NOT NULL UNIQUE,
    host_user_id UUID NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_meeting_host_user FOREIGN KEY (host_user_id) REFERENCES tbl_user(id)
);

-- 会议成员表：记录谁加入了会议
CREATE TABLE IF NOT EXISTS tbl_meeting_member (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL,
    user_id UUID NOT NULL,
    peer_id VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    left_at TIMESTAMP,
    status SMALLINT NOT NULL DEFAULT 1,
    CONSTRAINT fk_member_meeting FOREIGN KEY (meeting_id) REFERENCES tbl_meeting(id),
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES tbl_user(id)
);

-- 会议记录表：用于历史与审计（进入/离开/关闭等）
CREATE TABLE IF NOT EXISTS tbl_meeting_record (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL,
    user_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    action_time TIMESTAMP NOT NULL DEFAULT NOW(),
    detail VARCHAR(500),
    CONSTRAINT fk_record_meeting FOREIGN KEY (meeting_id) REFERENCES tbl_meeting(id),
    CONSTRAINT fk_record_user FOREIGN KEY (user_id) REFERENCES tbl_user(id)
);
