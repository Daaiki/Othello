package client;

import java.net.*;
import java.io.*;
import javax.swing.*;
import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class MyClient extends JFrame implements MouseListener, MouseMotionListener {
	private JButton buttonArray[][];
	private JLabel turn;
	private JLabel counterWhite, counterBlack;
	private JLabel blackIconLabel, whiteIconLabel;
	private JLabel arrowLabel;
	private ImageIcon black, white, board;
	private ImageIcon myIcon, yourIcon;
	private ImageIcon blackIconImage, whiteIconImage;
	private ImageIcon arrow;
	private int myColor;
	private int myTurn;
	private boolean isWhiteTurn = false;
  private Container c;
  PrintWriter out;

	public MyClient() {
		//名前の入力ダイアログを開く
		String myName = JOptionPane.showInputDialog(null, "名前を入力してください", "名前の入力", JOptionPane.QUESTION_MESSAGE);
		// 名前の入力がないとき
    if (myName.equals("")) {
			myName = "No name";
    }
    
    // IPアドレスを入力するダイアログを開く
		String myIpAddress = JOptionPane.showInputDialog(null, "IPアドレスを入力してください", "IPアドレスの入力", JOptionPane.QUESTION_MESSAGE);
    if (myIpAddress.equals("")) {
			myIpAddress = "localhost";
    }

		//ウィンドウを作成する
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Othello Game");
		setSize(1000, 750);
		c = getContentPane();

		white = new ImageIcon("./resources/white.png");
		black = new ImageIcon("./resources/black.png");
		board = new ImageIcon("./resources/board.png");
		
		// 自動レイアウトの設定を行わない
    c.setLayout(null);
		
		// 盤面を作成する
		buttonArray = new JButton[8][8];
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				buttonArray[i][j] = new JButton(board);
				// ボーダーを消す
				buttonArray[i][j].setBorderPainted(false);
				c.add(buttonArray[i][j]);
				buttonArray[i][j].setBounds(i*50+300, j*50+150, 50, 50);
				buttonArray[i][j].addMouseListener(this);
				buttonArray[i][j].setActionCommand(Integer.toString(j*8+i));
			}
		}
		
		// 初期配置
		initializeIcon(false);
		
		whiteIconImage = new ImageIcon("./resources/whiteIconImage.png");
		blackIconImage = new ImageIcon("./resources/blackIconImage.png");
		arrow = new ImageIcon("./resources/arrow.png");
		whiteIconLabel = new JLabel(whiteIconImage);
		blackIconLabel = new JLabel(blackIconImage);
		arrowLabel = new JLabel(arrow);
		c.add(whiteIconLabel);
		c.add(blackIconLabel);
		c.add(arrowLabel);
		whiteIconLabel.setBounds(60, 230, 60, 60);
		blackIconLabel.setBounds(170, 230, 60, 60);
		arrowLabel.setBounds(30, 243, 30, 30);
		
		// どっちのターンかを画面に表示する
		turn = new JLabel("You");
		c.add(turn);
		turn.setFont((new Font("Noto Sans", Font.BOLD, 18)));
		turn.setForeground(new Color(133,133,133));

		// どちらの駒が何個あるかを画面に表示する。
		counterWhite = new JLabel("2");
		counterBlack = new JLabel("2");
		c.add(counterWhite);
		c.add(counterBlack);
		counterWhite.setFont((new Font("Noto Sans JP", Font.BOLD, 18)));
		counterBlack.setFont((new Font("Noto Sans JP", Font.BOLD, 18)));
		counterWhite.setForeground(new Color(133,133,133));
		counterBlack.setForeground(new Color(133,133,133));
		counterWhite.setBounds(85, 280, 50, 45);
		counterBlack.setBounds(195, 280, 50, 45);

		//サーバに接続する
		Socket socket = null;
		try {
			socket = new Socket("localhost", 10000);
		} catch (UnknownHostException e) {
			System.err.println("ホストの IP アドレスが判定できません: " + e);
		} catch (IOException e) {
			 System.err.println("エラーが発生しました: " + e);
		}
		
		MesgRecvThread mrt = new MesgRecvThread(socket, myName);
		mrt.start();
	}
  
	//メッセージ受信のためのスレッド
	public class MesgRecvThread extends Thread {
		
		Socket socket;
		String myName;
		
		public MesgRecvThread(Socket s, String n){
			socket = s;
			myName = n;
		}
		
		//通信状況を監視し，受信データによって動作する
		public void run() {
			try{
				InputStreamReader sisr = new InputStreamReader(socket.getInputStream());
				BufferedReader br = new BufferedReader(sisr);
				out = new PrintWriter(socket.getOutputStream(), true);
				out.println(myName);
				// サーバーが送った番号をint型で受け取る
				int myNumberInt = Integer.parseInt(br.readLine());
				
				// サーバーが送った数値の偶奇で黒か白かを変える
				if (myNumberInt % 2 == 0) {
					myColor = 0;
					myTurn = 0;
					myIcon = black;
					yourIcon = white;
					turn.setBounds(183, 190, 60, 30);
				} else {
					myColor = 1;
					myTurn = 1;
					myIcon = white;
					yourIcon = black;
					turn.setBounds(73, 190, 60, 30);
				}

				while (true) {
					String inputLine = br.readLine();
					if (inputLine != null) {
						String[] inputTokens = inputLine.split(" ");
						String cmd = inputTokens[0];

						if (cmd.equals("PASS")) {
							myTurn = 1 - myTurn;
							isWhiteTurn = !isWhiteTurn;
							changeTurn(isWhiteTurn);
						}

						// 駒を置く処理
						if (cmd.equals("PLACE")) {
							// PLACE ボタンの番号 色 
							int theButtonName = Integer.parseInt(inputTokens[1]);
							int theColor = Integer.parseInt(inputTokens[2]);

							// 座標に戻す
							int x = theButtonName / 8;
							int y = theButtonName % 8;

							// アイコンを変更する
							if (theColor == myColor) {
								// 送信元クライアントでの処理
								buttonArray[y][x].setIcon(myIcon);
							} else {
								// 送信先クライアントでの処理
								buttonArray[y][x].setIcon(yourIcon);
							}

							// 駒数の表示を変更
							counterWhite.setText(Integer.toString(howManyIconExists()[0]));
							counterBlack.setText(Integer.toString(howManyIconExists()[1]));
							
							
							// ターン切り替え
							myTurn = 1 - myTurn;
							isWhiteTurn = !isWhiteTurn;
							changeTurn(isWhiteTurn);

							// パスが発生するか判定
							isPass();
						}

						// 駒をひっくり返す処理
						if (cmd.equals("FLIP")) {
							// FLIP ボタンの番号 色 
							int theButtonName = Integer.parseInt(inputTokens[1]);
							int theColor = Integer.parseInt(inputTokens[2]);

							// 座標に戻す
							int x = theButtonName / 8;
							int y = theButtonName % 8;

							// アイコンを変更する
							if (theColor == myColor) {
								// 送信元クライアントでの処理
								buttonArray[y][x].setIcon(myIcon);
							} else {
								// 送信先クライアントでの処理
								buttonArray[y][x].setIcon(yourIcon);
							}
						}
					} else {
						break;
					}
				}
				socket.close();
			} catch (IOException e) {
				System.err.println("エラーが発生しました: " + e);
			}
		}
	}

	public static void main(String[] args) {
		MyClient net = new MyClient();
		net.setVisible(true);
	}

	// クリックしたときの処理
	public void mouseClicked(MouseEvent e) {
		JButton theButton = (JButton) e.getComponent();
		// アイコンの配列の番号を取得
		int theArrayIndex = Integer.parseInt(theButton.getActionCommand());
		Icon theIcon = theButton.getIcon();

		if (myTurn == 1 && theIcon == board) {
			// 座標に戻す
			int x = theArrayIndex / 8;
			int y = theArrayIndex % 8;

			if (judgeButton(y, x)) {
				// 置ける
				String msg = "PLACE" + " " + theArrayIndex + " " + myColor;
				
				// サーバに情報を送る
				out.println(msg);
				out.flush();

				// 画面のオブジェクトを描画しなおす
				repaint();

			}
		}	
	}

	// 裏返りの発生する駒かどうかの判定
	public boolean judgeButton(int y, int x) {
		boolean flag = false;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (i == 0 && j == 0) continue;
				if (y+j < 0 || y+j >= 8 || x+i < 0 || x+i >= 8) continue;
				
				// ひっくり返せる駒が一つ以上あれば
				if (flipButtons(y, x, j, i, true) >= 1) {
					flag = true;
				}
			}
		}
		return flag;
	}

	// 一方向にある駒群を裏返す命令を送る
	public int flipButtons(int y, int x, int j, int i, boolean isFlip) {
		int flipNum = 0;
		int k;

		for (int dy = j, dx = i; ; dy += j, dx += i) {
			// 場外
			if (y+dy < 0 || y+dy >= 8 || x+dx < 0 || x+dx >= 8) {
				return 0;
			}
			// この位置のアイコンを取得する
			Icon icon = buttonArray[y+dy][x+dx].getIcon();

			if (icon == board) {
				return 0;
			} else if (icon == myIcon) {
				if (!isFlip) return flipNum;
				for (dy = j, dx=i, k=0; k<flipNum; k++, dy+=j, dx+=i) {
					//ボタンの位置情報を作る
					int msgy = y + dy;
					int msgx = x + dx;
					int theArrayIndex = msgy + msgx*8;
					
					//サーバに情報を送る
					String msg = "FLIP"+" "+theArrayIndex+" "+myColor;
					out.println(msg);
					out.flush();
				}
				return flipNum;
			} else if (icon == yourIcon) {
				flipNum += 1;
			}
		} 
	}

	public boolean canSetIcon(int y, int x) {
		boolean flag = false;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				if (i == 0 && j == 0) continue;
				if (y+j < 0 || y+j >= 8 || x+i < 0 || x+i >= 8) continue;
				
				// ひっくり返せる駒が一つ以上あれば
				if (flipButtons(y, x, j, i, false) >= 1) {
					flag = true;
				}
			}
		}
		return flag;
	}

	public boolean isPass() {
		if (myTurn != 1) return false;

		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (buttonArray[j][i].getIcon() != board) continue;
				if (canSetIcon(j, i)) {
					return false;
				}
			}
		}
		// パス
		String msg = "PASS";
		// サーバに情報を送る
		out.println(msg);
		out.flush();

		return true;
	}

	public int[] howManyIconExists() {
		// [white, black]
		int[] counter = new int[2];
		
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				Icon icon = buttonArray[i][j].getIcon();
				if (icon == white) {
					counter[0]++;
				} else if (icon == black) {
					counter[1]++;
				}
			}
		}
		
		return counter;
	}

	public void initializeIcon(boolean isDebug) {
		if (isDebug) {
			// 自動パス実装用
			for (int x = 0; x < 8; x++) {
				buttonArray[x][1].setIcon(white);
				buttonArray[x][0].setIcon(white);
				buttonArray[x][2].setIcon(black);
			}
			return;
		}
		buttonArray[4][3].setIcon(white);
		buttonArray[4][4].setIcon(black);
		buttonArray[3][4].setIcon(white);
		buttonArray[3][3].setIcon(black);
	} 

	public void changeTurn(boolean isWhite) {
		if (isWhite) {
			arrowLabel.setBounds(140, 243, 30, 30);
		} else {
			arrowLabel.setBounds(30, 243, 30, 30);
		}
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseDragged(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
}