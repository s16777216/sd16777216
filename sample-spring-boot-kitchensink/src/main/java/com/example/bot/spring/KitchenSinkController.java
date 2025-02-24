package com.example.bot.spring;

import static java.util.Collections.singletonList;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.common.io.ByteStreams;

import com.linecorp.bot.client.LineBlobClient;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.DatetimePickerAction;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.BeaconEvent;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.JoinEvent;
import com.linecorp.bot.model.event.MemberJoinedEvent;
import com.linecorp.bot.model.event.MemberLeftEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.UnfollowEvent;
import com.linecorp.bot.model.event.UnknownEvent;
import com.linecorp.bot.model.event.UnsendEvent;
import com.linecorp.bot.model.event.VideoPlayCompleteEvent;
import com.linecorp.bot.model.event.message.AudioMessageContent;
import com.linecorp.bot.model.event.message.ContentProvider;
import com.linecorp.bot.model.event.message.FileMessageContent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.LocationMessageContent;
import com.linecorp.bot.model.event.message.StickerMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.message.VideoMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.group.GroupMemberCountResponse;
import com.linecorp.bot.model.group.GroupSummaryResponse;
import com.linecorp.bot.model.message.AudioMessage;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.ImagemapMessage;
import com.linecorp.bot.model.message.LocationMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.VideoMessage;
import com.linecorp.bot.model.message.imagemap.ImagemapArea;
import com.linecorp.bot.model.message.imagemap.ImagemapBaseSize;
import com.linecorp.bot.model.message.imagemap.ImagemapExternalLink;
import com.linecorp.bot.model.message.imagemap.ImagemapVideo;
import com.linecorp.bot.model.message.imagemap.MessageImagemapAction;
import com.linecorp.bot.model.message.imagemap.URIImagemapAction;
import com.linecorp.bot.model.message.sender.Sender;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.message.template.ImageCarouselColumn;
import com.linecorp.bot.model.message.template.ImageCarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.model.room.RoomMemberCountResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LineMessageHandler
public class KitchenSinkController {
  @Autowired
  private LineMessagingClient lineMessagingClient;
  
  @Autowired
  private LineBlobClient lineBlobClient;
  
  @EventMapping
  public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
    TextMessageContent message = event.getMessage();
    handleTextContent(event.getReplyToken(), event, message);
  }
  
  @EventMapping
  public void handleStickerMessageEvent(MessageEvent<StickerMessageContent> event) {
    handleSticker(event.getReplyToken(), event.getMessage());
  }
  
  @EventMapping
  public void handleLocationMessageEvent(MessageEvent<LocationMessageContent> event) {
    LocationMessageContent locationMessage = event.getMessage();
    reply(event.getReplyToken(), new LocationMessage(
    locationMessage.getTitle(),
    locationMessage.getAddress(),
    locationMessage.getLatitude(),
    locationMessage.getLongitude()
    ));
  }
  
  @EventMapping
  public void handleUnfollowEvent(UnfollowEvent event) {
    log.info("unfollowed this bot: {}", event);
  }
  
  @EventMapping
  public void handleUnknownEvent(UnknownEvent event) {
    log.info("Got an unknown event!!!!! : {}", event);
  }
  
  @EventMapping
  public void handleFollowEvent(FollowEvent event) {
    String replyToken = event.getReplyToken();
    this.replyText(replyToken, "Got followed event");
  }
  
  @EventMapping    //bot join event
  public void handleJoinEvent(JoinEvent event) {
    String replyToken = event.getReplyToken();
    this.reply(
    replyToken,
    Arrays.asList(new TextMessage("看到我大槌尾鰭都不會怕的"),
    new TextMessage("還敢邀我進來阿"))
    );
  }
  
  @EventMapping
  public void handlePostbackEvent(PostbackEvent event) {
    String replyToken = event.getReplyToken();
    this.replyText(replyToken,
    "Got postback data " + event.getPostbackContent().getData() + ", param " + event
    .getPostbackContent().getParams().toString());
  }
  
  @EventMapping
  public void handleBeaconEvent(BeaconEvent event) {
    String replyToken = event.getReplyToken();
    this.replyText(replyToken, "Got beacon message " + event.getBeacon().getHwid());
  }
  
  @EventMapping
  public void handleMemberJoined(MemberJoinedEvent event) {
    final String userId = event.getSource().getUserId();
    String replyToken = event.getReplyToken();
    this.reply(
    replyToken,
    new TextMessage("又一個不怕死的進來了")
    );
    
  }
  
  @EventMapping
  public void handleMemberLeft(MemberLeftEvent event) {
    log.info("Got memberLeft message: {}", event.getLeft().getMembers()
    .stream().map(Source::getUserId)
    .collect(Collectors.joining(",")));
  }

  @EventMapping
  public void handleMemberLeft(UnsendEvent event) {
    log.info("Got unsend event: {}", event);
  }

  @EventMapping
  public void handleOtherEvent(Event event) {
    log.info("Received message(Ignored): {}", event);
  }

  private void reply(@NonNull String replyToken, @NonNull Message message) {
    reply(replyToken, singletonList(message));
  }

  private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
    reply(replyToken, messages, false);
  }

  private void reply(@NonNull String replyToken,
  @NonNull List<Message> messages,
  boolean notificationDisabled) {
    try {
      BotApiResponse apiResponse = lineMessagingClient
      .replyMessage(new ReplyMessage(replyToken, messages, notificationDisabled))
      .get();
      log.info("Sent messages: {}", apiResponse);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private void replyText(@NonNull String replyToken, @NonNull String message) {
    if (replyToken.isEmpty()) {
      throw new IllegalArgumentException("replyToken must not be empty");
    }
    if (message.length() > 1000) {
      message = message.substring(0, 1000 - 2) + "……";
    }
    this.reply(replyToken, new TextMessage(message));
  }

  private void handleHeavyContent(String replyToken, String messageId,
  Consumer<MessageContentResponse> messageConsumer) {
    final MessageContentResponse response;
    try {
      response = lineBlobClient.getMessageContent(messageId)
      .get();
    } catch (InterruptedException | ExecutionException e) {
      reply(replyToken, new TextMessage("Cannot get image: " + e.getMessage()));
      throw new RuntimeException(e);
    }
    messageConsumer.accept(response);
  }

  private void handleSticker(String replyToken, StickerMessageContent content) {
    reply(replyToken, new StickerMessage(
    content.getPackageId(), content.getStickerId())
    );
  }

  private void handleTextContent(String replyToken, Event event, TextMessageContent content) throws Exception {
    final String text = content.getText();
    log.info("Got text message from replyToken:{}: text:{} emojis:{}", replyToken, text, content.getEmojis());
    String[] textarr = text.split(" ");
    String text01 = textarr[0].substring(0,2), text02 = textarr[0].substring(2);
    if ( text01.equals("尾鰭")&&text.substring(2)!="") {
      switch (text02) {
        case "profile": {
          log.info("Invoking 'profile' command: source:{}", event.getSource());
            final String userId = event.getSource().getUserId();
            if (userId != null) {
              if (event.getSource() instanceof GroupSource) {
                lineMessagingClient
                .getGroupMemberProfile(((GroupSource) event.getSource()).getGroupId(), userId)
                .whenComplete((profile, throwable) -> {
                if (throwable != null) {
                this.replyText(replyToken, throwable.getMessage());
                return;
                }
                
                this.reply(
                replyToken,
                Arrays.asList(new TextMessage("(from group)"),
                new TextMessage(
                "Display name: " + profile.getDisplayName()),
                new ImageMessage(profile.getPictureUrl(),
                profile.getPictureUrl()))
                );
                });
              } else {
                lineMessagingClient
                .getProfile(userId)
                .whenComplete((profile, throwable) -> {
                  if (throwable != null) {
                    this.replyText(replyToken, throwable.getMessage());
                    return;
                  }
                  
                  this.reply(
                  replyToken,
                  Arrays.asList(new TextMessage(
                  "Display name: " + profile.getDisplayName()),
                  new TextMessage("Status message: "
                  + profile.getStatusMessage()))
                  );
                  
                });
              }
            } else {
              this.replyText(replyToken, "Bot can't use profile API without user ID");
            }
            break;
          }
        case "滾蛋": {
          Source source = event.getSource();
            if (source instanceof GroupSource) {
              this.replyText(replyToken, "真是令人遺憾" );
              lineMessagingClient.leaveGroup(((GroupSource) source).getGroupId()).get();
            } else if (source instanceof RoomSource) {
                this.replyText(replyToken, "真是令人遺憾");
                lineMessagingClient.leaveRoom(((RoomSource) source).getRoomId()).get();
              } else {
                this.replyText(replyToken, "令人遺憾的結果");
              }
            break;
          }
        case "吃飯": {
          //String[] food = text03.split(" ");
            if (textarr.length==1) {
              this.replyText(
              replyToken,
              "不說要吃什麼是要吃洨喔"
              );
            } else {
              Random rand = new Random();
              ArrayList<String> foodarr = new ArrayList<String>(Arrays.asList(textarr));
              foodarr.add("屎");
              String finalfood = foodarr.get(rand.nextInt(textarr.length)+1),food1="";
              for (int i=1; i<textarr.length; i++) {
                food1 = food1+i+"."+textarr[i]+" ";
              }  
              ConfirmTemplate confirmTemplate = new ConfirmTemplate(
              food1+"是嗎?",
              new MessageAction("Yes", "尾鰭說吃"+finalfood),
              new MessageAction("No", "尾鰭不對")
              );
              TemplateMessage templateMessage = new TemplateMessage("Confirm alt text", confirmTemplate);
              this.reply(replyToken, templateMessage);
            } // end of if-else
            
            break;
          }
        case "你能幹嘛": {
          URI imageUrl = createUri("/static/buttons/jelly.gif");
            ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
            imageUrl,
            "我能幹嘛?",
            "你問這什麼爛問題",
            Arrays.asList(
            new PostbackAction("罵尾鰭",
            "尾鰭幹你娘",
            "尾鰭幹你娘"),
            new PostbackAction("幫大家選擇要吃啥",
            "尾鰭吃飯",
            "尾鰭吃飯"),
            new PostbackAction("你是誰?",
            "尾鰭你是誰",
            "尾鰭你是誰"),
            new MessageAction("你完蛋了",
            "尾鰭完了")
            ));
            TemplateMessage templateMessage = new TemplateMessage("尾鰭能幹嘛", buttonsTemplate);
            this.reply(replyToken, templateMessage);
            break;
          }
        case "帥嗎": {
          ConfirmTemplate confirmTemplate = new ConfirmTemplate(
            "我帥不帥?",
            new MessageAction("帥","尾鰭帥"),
            new MessageAction("醜", "尾鰭醜")
            );
            TemplateMessage templateMessage = new TemplateMessage("美醜大對決", confirmTemplate);
            this.reply(replyToken, templateMessage);
            break;
          }  
        case "好康":{
          //            final String baseUrl,
          //            final String altText,
          //            final ImagemapBaseSize imagemapBaseSize,
          //            final List<ImagemapAction> actions) {
          this.reply(replyToken, ImagemapMessage
            .builder()
            .baseUrl(createUri("/static/imagemap"))
            .altText("尾鰭傳好康ㄉ")
            .baseSize(new ImagemapBaseSize(1040, 1040))
            .actions(Arrays.asList(
            URIImagemapAction.builder()
            .linkUri("https://youtu.be/072tU1tamd0")
            .area(new ImagemapArea(0, 0, 520, 520))
            .build(),
            MessageImagemapAction.builder()
            .text("我喜歡肛交")
            .area(new ImagemapArea(520, 0, 520, 520))
            .build(),
            URIImagemapAction.builder()
            .linkUri("https://www.instagram.com/miakhalifa/")
            .area(new ImagemapArea(0, 520, 520, 520))
            .build(),
            MessageImagemapAction.builder()
            .text("我是臭ㄐㄐ")
            .area(new ImagemapArea(520, 520, 520, 520))
            .build()
            ))
            .build());
            break;
          }
        case "完了":{
          this.replyText(
            replyToken,
            "裂開"
            );
            break;
          }
        case "幹你娘": {
          this.replyText(
            replyToken,
            "它奶奶的槌子"
            );
            break;
          }
        case "你是誰": {
          this.replyText(
            replyToken,
            "我是大雞癢"
            );
            break;
          }
        case "不對": {
          this.replyText(
            replyToken,
            "啊不然勒"
            );
            break;
          }
        case "帥": {
          this.replyText(
            replyToken,
            "牛逼"
            );                                                                    
            break;
          }
        case "醜": {
          Random rand = new Random();
            if (rand.nextInt(2)==0) {
              this.reply(
              replyToken,
              Arrays.asList(
              new TextMessage("你才醜"),
              new TextMessage("你全家都醜") ,
              new TextMessage("你祖宗十八代才醜")
              ));
            } else {
              log.info("Invoking 'profile' command: source:{}", event.getSource());
              final String userId = event.getSource().getUserId();
              if (userId != null) {
                if (event.getSource() instanceof GroupSource) {
                  lineMessagingClient
                  .getGroupMemberProfile(((GroupSource) event.getSource()).getGroupId(), userId)
                  .whenComplete((profile, throwable) -> {
                  if (throwable != null) {
                  this.replyText(replyToken, throwable.getMessage());
                  return;
                  }
                  
                  this.reply(replyToken,
                  TextMessage.builder()
                  .text("我也很醜")
                  .sender(Sender.builder()
                  .name(profile.getDisplayName())
                  .iconUrl(profile.getPictureUrl())
                  .build())
                  .build());
                  });
                }
              } else {
                this.reply(
                replyToken,
                Arrays.asList(
                new TextMessage("你才醜"),
                new TextMessage("你全家都醜") ,
                new TextMessage("你祖宗十八代才醜")
                ));
              }
            } // end of if-else
            
            break;
          }  
        case "變身":{
          log.info("Invoking 'profile' command: source:{}", event.getSource());
            final String userId = event.getSource().getUserId();
            if (userId != null) {
              if (event.getSource() instanceof GroupSource) {
                lineMessagingClient
                .getGroupMemberProfile(((GroupSource) event.getSource()).getGroupId(), userId)
                .whenComplete((profile, throwable) -> {
                if (throwable != null) {
                this.replyText(replyToken, throwable.getMessage());
                return;
                }
                String saying = new String();
                switch (profile.getDisplayName()) {
                case "羅緯琦" : 
                saying = "牛逼";  
                break;
                case "邱文瑞" : 
                saying = "難過";
                break;
                case "呂昊曈":
                saying = "沒水準";
                break;
                case "黃韋晧":
                saying = "快點";
                break;
                default:
                saying = "開bang";
                break;
                }
                
                this.reply(replyToken,
                TextMessage.builder()
                .text(saying)
                .sender(Sender.builder()
                .name(profile.getDisplayName())
                .iconUrl(profile.getPictureUrl())
                .build())
                .build());
                });
              } else {
                lineMessagingClient
                .getProfile(userId)
                .whenComplete((profile, throwable) -> {
                  if (throwable != null) {
                    this.replyText(replyToken, throwable.getMessage());
                    return;
                  }
                  
                  this.reply(
                  replyToken,
                  Arrays.asList(new TextMessage(
                  "Display name: " + profile.getDisplayName()),
                  new TextMessage("Status message: "
                  + profile.getStatusMessage()))
                  );
                  
                });
              }
            } else {
              this.replyText(replyToken, "Bot can't use profile API without user ID");
            }
            break;
          }
        case "flex":{
          this.reply(replyToken, new Schedule().get());
            break;
          }
        default:{
          log.info("Returns message {}: {}", replyToken, text);
            this.replyText(
            replyToken,
            "阿這"
            );
          break;}
      }
    }
    if (text01.equals("阿這")) {
      this.replyText(
      replyToken,
      "還敢學阿"
      );
    } // end of if
  }
            
  private static URI createUri(String path) {
    return ServletUriComponentsBuilder.fromCurrentContextPath()
    .scheme("https")
    .path(path).build()
    .toUri();
  }
            
  private void system(String... args) {
    ProcessBuilder processBuilder = new ProcessBuilder(args);
    try {
      Process start = processBuilder.start();
      int i = start.waitFor();
      log.info("result: {} =>  {}", Arrays.toString(args), i);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      log.info("Interrupted", e);
      Thread.currentThread().interrupt();
    }
  }
            
  private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
    log.info("Got content-type: {}", responseBody);
    
    DownloadedContent tempFile = createTempFile(ext);
    try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
      ByteStreams.copy(responseBody.getStream(), outputStream);
      log.info("Saved {}: {}", ext, tempFile);
      return tempFile;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
            
  private static DownloadedContent createTempFile(String ext) {
    String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID() + '.' + ext;
    Path tempFile = KitchenSinkApplication.downloadedContentDir.resolve(fileName);
    tempFile.toFile().deleteOnExit();
    return new DownloadedContent(
    tempFile,
    createUri("/downloaded/" + tempFile.getFileName()));
  }
            
  @Value
  private static class DownloadedContent {
    Path path;
    URI uri;
  }
}
